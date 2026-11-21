package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Palladium 4.5.10 compatibility for its player FlightHandler on Minecraft
 * 1.21.1. EntityDimensions.height is now a record accessor, and a newly
 * activated flight type needs a few ticks to lift a grounded player before
 * the original grounded-flight cancellation can run.
 */
final class PalladiumFlightHandler1201 implements Opcodes {
    static final String FLIGHT_HANDLER = "net/threetag/palladium/entity/FlightHandler";
    static final String ENTITY_DIMENSIONS = "net/minecraft/world/entity/EntityDimensions";
    static final String FLIGHT_TYPE = FLIGHT_HANDLER + "$FlightType";
    static final String PLAYER = "net/minecraft/world/entity/player/Player";
    static final String GROUND_TAKEOFF_GRACE = "legacyconnector$groundTakeoffGrace";
    static final int GROUND_TAKEOFF_GRACE_TICKS = 6;

    private PalladiumFlightHandler1201() {
    }

    static int apply(ClassNode node) {
        if (!FLIGHT_HANDLER.equals(node.name)) {
            return 0;
        }

        int changed = 0;
        for (var method : node.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (var instruction = method.instructions.getFirst(); instruction != null;) {
                var next = instruction.getNext();
                if (instruction instanceof FieldInsnNode field
                        && field.getOpcode() == GETFIELD
                        && ENTITY_DIMENSIONS.equals(field.owner)
                        && "F".equals(field.desc)
                        && ("height".equals(field.name) || "f_20378_".equals(field.name))) {
                    method.instructions.set(field,
                            new MethodInsnNode(INVOKEVIRTUAL, ENTITY_DIMENSIONS, "height", "()F", false));
                    changed++;
                }
                instruction = next;
            }
        }
        return changed;
    }

    /**
     * Palladium's original FlightHandler.tick() drops an activated flight type
     * on the very first tick that still sees onGround(). A double-jump takeoff
     * can activate the handler before the jump impulse has moved the player off
     * the floor. Keep that activation alive for six grounded ticks so its
     * existing updateMovement() can apply the Jump-key lift. As soon as the
     * player leaves the ground, the grace is cleared; the normal landing,
     * water, fall-flying and attribute-loss cancellation remains unchanged.
     */
    static int applyGroundTakeoffGrace(ClassNode node) {
        if (!FLIGHT_HANDLER.equals(node.name)) {
            return 0;
        }

        String graceDesc = "I";
        FieldNode graceField = null;
        for (FieldNode field : node.fields) {
            if (GROUND_TAKEOFF_GRACE.equals(field.name)) {
                if (!graceDesc.equals(field.desc)) {
                    throw new IllegalStateException(
                            "Palladium FlightHandler has an unexpected ground takeoff grace field descriptor: "
                                    + field.desc);
                }
                graceField = field;
            }
        }

        String setterDesc = "(L" + FLIGHT_TYPE + ";)V";
        MethodNode setter = find(node, "setFlightType", setterDesc);
        MethodNode tick = find(node, "tick", "()V");
        if (setter == null || tick == null || tick.instructions == null || setter.instructions == null) {
            throw new IllegalStateException(
                    "Palladium FlightHandler is missing setFlightType(FlightType) or tick() for the grounded takeoff adapter");
        }

        if (graceField != null) {
            if (setterUsesGrace(setter) && tickUsesGrace(tick) && hasAdaptedGroundCheck(tick)) {
                return 0;
            }
            throw new IllegalStateException(
                    "Palladium FlightHandler contains a partial ground takeoff grace migration");
        }

        MethodInsnNode onGround = findSingleOnGroundCall(tick);
        AbstractInsnNode branchInstruction = nextReal(onGround);
        if (!(branchInstruction instanceof JumpInsnNode originalGroundBranch)
                || originalGroundBranch.getOpcode() != IFNE) {
            throw new IllegalStateException(
                    "Palladium FlightHandler.tick() has an unexpected branch after its onGround() check");
        }

        List<AbstractInsnNode> returns = new ArrayList<>();
        for (AbstractInsnNode instruction : setter.instructions) {
            if (instruction.getOpcode() == RETURN) returns.add(instruction);
        }
        if (returns.size() != 1) {
            throw new IllegalStateException(
                    "Palladium FlightHandler.setFlightType() expected one return, found " + returns.size());
        }

        node.fields.add(new FieldNode(
                ACC_PRIVATE | ACC_SYNTHETIC,
                GROUND_TAKEOFF_GRACE,
                graceDesc,
                null,
                null));

        injectGraceInitialization(setter, returns.get(0), node.name);
        adaptGroundCheck(tick, originalGroundBranch, node.name);
        return 1;
    }

    private static void injectGraceInitialization(MethodNode setter, AbstractInsnNode returnInstruction, String owner) {
        LabelNode clearGrace = new LabelNode();
        LabelNode done = new LabelNode();
        InsnList code = new InsnList();

        code.add(new VarInsnNode(ALOAD, 1));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, FLIGHT_TYPE, "isNotNull", "()Z", false));
        code.add(new JumpInsnNode(IFEQ, clearGrace));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new FieldInsnNode(GETFIELD, owner, "player", "L" + PLAYER + ";"));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, PLAYER, "onGround", "()Z", false));
        code.add(new JumpInsnNode(IFEQ, clearGrace));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new IntInsnNode(BIPUSH, GROUND_TAKEOFF_GRACE_TICKS));
        code.add(new FieldInsnNode(PUTFIELD, owner, GROUND_TAKEOFF_GRACE, "I"));
        code.add(new JumpInsnNode(GOTO, done));

        code.add(clearGrace);
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new InsnNode(ICONST_0));
        code.add(new FieldInsnNode(PUTFIELD, owner, GROUND_TAKEOFF_GRACE, "I"));
        code.add(done);

        setter.instructions.insertBefore(returnInstruction, code);
        setter.maxStack = Math.max(setter.maxStack, 2);
    }

    private static void adaptGroundCheck(MethodNode tick, JumpInsnNode originalBranch, String owner) {
        LabelNode airborne = new LabelNode();
        LabelNode groundCheckComplete = new LabelNode();
        InsnList code = new InsnList();

        // false means the player has already lifted off: clear the one-time
        // grace so a later landing still follows Palladium's original behavior.
        code.add(new JumpInsnNode(IFEQ, airborne));

        // Still grounded. Cancel only after the takeoff grace has expired.
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new FieldInsnNode(GETFIELD, owner, GROUND_TAKEOFF_GRACE, "I"));
        code.add(new JumpInsnNode(IFLE, originalBranch.label));

        // Consume one grace tick while the movement controller applies lift.
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new InsnNode(DUP));
        code.add(new FieldInsnNode(GETFIELD, owner, GROUND_TAKEOFF_GRACE, "I"));
        code.add(new InsnNode(ICONST_1));
        code.add(new InsnNode(ISUB));
        code.add(new FieldInsnNode(PUTFIELD, owner, GROUND_TAKEOFF_GRACE, "I"));
        code.add(new JumpInsnNode(GOTO, groundCheckComplete));

        code.add(airborne);
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new InsnNode(ICONST_0));
        code.add(new FieldInsnNode(PUTFIELD, owner, GROUND_TAKEOFF_GRACE, "I"));
        code.add(groundCheckComplete);

        tick.instructions.insertBefore(originalBranch, code);
        tick.instructions.remove(originalBranch);
        tick.maxStack = Math.max(tick.maxStack, 3);
    }


    /**
     * Installs opt-in traces at the server/client flight state transitions. The helper is a
     * no-op unless -Dlegacyconnector.debug.palladiumFlight=true is supplied at launch.
     */
    static int applyDiagnostics(ClassNode node) {
        if (!FLIGHT_HANDLER.equals(node.name)) return 0;

        int changed = 0;
        MethodNode setter = find(node, "setFlightType", "(L" + FLIGHT_TYPE + ";)V");
        if (setter != null && !hasDiagnosticCall(setter, "logFlightType")) {
            InsnList trace = new InsnList();
            trace.add(new LdcInsnNode("setFlightType"));
            trace.add(new VarInsnNode(ALOAD, 0));
            trace.add(new VarInsnNode(ALOAD, 1));
            trace.add(new MethodInsnNode(INVOKESTATIC, "legacyconnector/v1_20_1/PalladiumFlightDebugCompat",
                    "logFlightType", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", false));
            setter.instructions.insert(trace);
            setter.maxStack = Math.max(setter.maxStack, 3);
            changed++;
        }

        for (MethodNode method : node.methods) {
            if (method.instructions == null || !method.desc.equals("()V")) continue;
            if (!method.name.equals("tick") && !method.name.equals("updateMovement")) continue;
            if (hasDiagnosticCall(method, "logFlightState")) continue;

            InsnList trace = new InsnList();
            trace.add(new LdcInsnNode(method.name));
            trace.add(new VarInsnNode(ALOAD, 0));
            trace.add(new MethodInsnNode(INVOKESTATIC, "legacyconnector/v1_20_1/PalladiumFlightDebugCompat",
                    "logFlightState", "(Ljava/lang/String;Ljava/lang/Object;)V", false));
            method.instructions.insert(trace);
            method.maxStack = Math.max(method.maxStack, 2);
            changed++;
        }
        return changed;
    }

    private static boolean hasDiagnosticCall(MethodNode method, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && call.owner.equals("legacyconnector/v1_20_1/PalladiumFlightDebugCompat")
                    && call.name.equals(name)) return true;
        }
        return false;
    }

    private static MethodInsnNode findSingleOnGroundCall(MethodNode tick) {
        MethodInsnNode found = null;
        int count = 0;
        for (AbstractInsnNode instruction : tick.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKEVIRTUAL
                    && call.desc.equals("()Z")
                    && ("onGround".equals(call.name) || "m_20096_".equals(call.name))
                    && call.owner.startsWith("net/minecraft/world/entity/")) {
                found = call;
                count++;
            }
        }
        if (count != 1) {
            throw new IllegalStateException(
                    "Palladium FlightHandler.tick() expected one player onGround() call, found " + count);
        }
        return found;
    }

    private static boolean setterUsesGrace(MethodNode setter) {
        for (AbstractInsnNode instruction : setter.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == PUTFIELD
                    && GROUND_TAKEOFF_GRACE.equals(field.name)
                    && "I".equals(field.desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tickUsesGrace(MethodNode tick) {
        boolean read = false;
        boolean write = false;
        for (AbstractInsnNode instruction : tick.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && GROUND_TAKEOFF_GRACE.equals(field.name)
                    && "I".equals(field.desc)) {
                read |= field.getOpcode() == GETFIELD;
                write |= field.getOpcode() == PUTFIELD;
            }
        }
        return read && write;
    }

    private static boolean hasAdaptedGroundCheck(MethodNode tick) {
        for (AbstractInsnNode instruction : tick.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKEVIRTUAL
                    && call.desc.equals("()Z")
                    && ("onGround".equals(call.name) || "m_20096_".equals(call.name))) {
                AbstractInsnNode next = nextReal(call);
                return next instanceof JumpInsnNode jump && jump.getOpcode() == IFEQ
                        && tickUsesGrace(tick);
            }
        }
        return false;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction == null ? null : instruction.getNext();
        while (next != null && next.getOpcode() < 0) {
            next = next.getNext();
        }
        return next;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }
}
