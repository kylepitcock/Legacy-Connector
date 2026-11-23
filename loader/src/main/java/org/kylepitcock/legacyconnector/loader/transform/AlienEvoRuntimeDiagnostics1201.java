package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Opt-in runtime probes for the two unresolved AlienEvo runtime defects.
 *
 * These probes observe the existing result; they do not change animation,
 * swing, camera, or beam behavior. Enable them with
 * -Dlegacyconnector.debug.alienevoRuntime=true.
 */
final class AlienEvoRuntimeDiagnostics1201 implements Opcodes {
    private static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoRuntimeDiagnostics1201Runtime";
    private static final String KINECELERAN_RUN =
            "net/habb/alienevo/client/model/animation/aliens/kineceleran/KineceleranRunAnimation";
    private static final String FIRST_PERSON_ARM =
            "net/habb/alienevo/client/model/animation/FirstPersonArmAnimator";
    private static final String CANCEL_FIRST_PERSON_SWING =
            "net/habb/alienevo/mixin/client/CancelFirstPersonSwingMixin";
    private static final String BONE_ANCHORS = "net/habb/alienevo/client/BoneAnchors";

    private static final String ANIMATE_DESC =
            "(Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$Builder;"
                    + "Lnet/minecraft/client/player/AbstractClientPlayer;"
                    + "Lnet/minecraft/client/model/HumanoidModel;"
                    + "Lnet/threetag/palladium/client/model/animation/PalladiumAnimation$FirstPersonContext;F)V";
    private static final String APPLY_ARM_DESC =
            "(Lnet/minecraft/client/model/geom/ModelPart;Z)V";
    private static final String BONE_WORLD_DESC =
            "(ILjava/lang/String;ZFLorg/joml/Matrix3f;Lnet/minecraft/world/phys/Vec3;)"
                    + "Lnet/minecraft/world/phys/Vec3;";

    private AlienEvoRuntimeDiagnostics1201() {}

    static int apply(ClassNode node) {
        return switch (node.name) {
            case KINECELERAN_RUN -> traceKineceleranRun(node);
            case FIRST_PERSON_ARM -> traceFirstPersonArm(node);
            case CANCEL_FIRST_PERSON_SWING -> traceVanillaSwing(node);
            case BONE_ANCHORS -> traceBoneWorld(node);
            default -> 0;
        };
    }

    private static int traceKineceleranRun(ClassNode node) {
        MethodNode method = find(node, "animate", ANIMATE_DESC);
        if (method == null || method.instructions == null) {
            throw new IllegalStateException("AlienEvo KineceleranRunAnimation.animate has an unexpected method shape");
        }

        int changed = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != RETURN || hasTraceBeforeReturn(method, instruction, "traceKineceleranRun")) {
                continue;
            }
            InsnList trace = new InsnList();
            trace.add(new VarInsnNode(ALOAD, 1)); // Palladium animation builder
            trace.add(new VarInsnNode(ALOAD, 2)); // player
            trace.add(new VarInsnNode(ALOAD, 4)); // first-person context
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceKineceleranRun",
                    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false));
            method.instructions.insertBefore(instruction, trace);
            changed++;
        }
        if (changed == 0 && !hasAnyCall(method, "traceKineceleranRun")) {
            throw new IllegalStateException("AlienEvo KineceleranRunAnimation.animate has no return sites to trace");
        }
        method.maxStack = Math.max(method.maxStack, 3);
        return changed;
    }

    private static int traceFirstPersonArm(ClassNode node) {
        MethodNode method = find(node, "apply", APPLY_ARM_DESC);
        if (method == null || method.instructions == null) {
            throw new IllegalStateException("AlienEvo FirstPersonArmAnimator.apply has an unexpected method shape");
        }

        int returns = 0;
        int changed = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != RETURN) continue;
            returns++;
            if (hasTraceBeforeReturn(method, instruction, "traceFirstPersonArmApply")) continue;

            InsnList trace = new InsnList();
            trace.add(new VarInsnNode(ALOAD, 2)); // resolved PlayerModelPart
            trace.add(new VarInsnNode(ALOAD, 3)); // pending/tuned AnimData (may be null)
            trace.add(new VarInsnNode(ALOAD, 1)); // rendered ModelPart after application
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceFirstPersonArmApply",
                    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false));
            method.instructions.insertBefore(instruction, trace);
            changed++;
        }
        if (returns == 0) {
            throw new IllegalStateException("AlienEvo FirstPersonArmAnimator.apply has no return site to trace");
        }
        method.maxStack = Math.max(method.maxStack, 3);
        return changed;
    }

    private static int traceVanillaSwing(ClassNode node) {
        MethodNode method = find(node, "alienevo$cancelSwingProgress", "(F)F");
        if (method == null || method.instructions == null) {
            throw new IllegalStateException("AlienEvo CancelFirstPersonSwingMixin handler has an unexpected method shape");
        }

        int returns = 0;
        int changed = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != FRETURN) continue;
            returns++;
            if (hasTraceBeforeReturn(method, instruction, "traceVanillaSwing")) continue;

            InsnList trace = new InsnList();
            trace.add(new InsnNode(DUP)); // retain the result for FRETURN
            trace.add(new VarInsnNode(FLOAD, 1)); // original vanilla swing progress
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceVanillaSwing", "(FF)V", false));
            method.instructions.insertBefore(instruction, trace);
            changed++;
        }
        if (returns == 0) {
            throw new IllegalStateException("AlienEvo CancelFirstPersonSwingMixin has no FRETURN site to trace");
        }
        method.maxStack = Math.max(method.maxStack, 2);
        return changed;
    }

    private static int traceBoneWorld(ClassNode node) {
        MethodNode method = find(node, "boneWorld", BONE_WORLD_DESC);
        if (method == null || method.instructions == null || (method.access & ACC_STATIC) == 0) {
            throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld has an unexpected method shape");
        }

        int returns = 0;
        int changed = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != ARETURN) continue;
            returns++;
            if (hasTraceBeforeReturn(method, instruction, "traceBeamAnchor")
                    || hasTraceBeforeReturn(method, instruction, "traceMissingBeamAnchor")) {
                continue;
            }

            LabelNode missing = new LabelNode();
            LabelNode done = new LabelNode();
            InsnList trace = new InsnList();
            trace.add(new InsnNode(DUP));
            trace.add(new JumpInsnNode(IFNULL, missing));
            trace.add(new VarInsnNode(ASTORE, 8)); // preserve non-null world origin
            trace.add(new VarInsnNode(ILOAD, 0));
            trace.add(new VarInsnNode(ALOAD, 1));
            trace.add(new VarInsnNode(ILOAD, 2));
            trace.add(new VarInsnNode(FLOAD, 3));
            trace.add(new VarInsnNode(ALOAD, 4)); // inverse-view matrix passed by AlienEvo
            trace.add(new VarInsnNode(ALOAD, 5)); // camera position passed by AlienEvo
            trace.add(new VarInsnNode(ALOAD, 6)); // captured named-bone pose
            trace.add(new VarInsnNode(ALOAD, 7)); // scaled/rotated local translation
            trace.add(new VarInsnNode(ALOAD, 8)); // final world origin
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceBeamAnchor",
                    "(ILjava/lang/String;ZFLorg/joml/Matrix3f;Lnet/minecraft/world/phys/Vec3;"
                            + "Lorg/joml/Matrix4f;Lorg/joml/Vector3f;Lnet/minecraft/world/phys/Vec3;)V",
                    false));
            trace.add(new VarInsnNode(ALOAD, 8));
            trace.add(new JumpInsnNode(GOTO, done));
            trace.add(missing);
            trace.add(new InsnNode(POP));
            trace.add(new VarInsnNode(ILOAD, 0));
            trace.add(new VarInsnNode(ALOAD, 1));
            trace.add(new VarInsnNode(ILOAD, 2));
            trace.add(new MethodInsnNode(INVOKESTATIC, HELPER, "traceMissingBeamAnchor",
                    "(ILjava/lang/String;Z)V", false));
            trace.add(new InsnNode(ACONST_NULL));
            trace.add(done);
            method.instructions.insertBefore(instruction, trace);
            changed++;
        }
        if (returns == 0) {
            throw new IllegalStateException("AlienEvo BoneAnchors.boneWorld has no ARETURN site to trace");
        }
        method.maxLocals = Math.max(method.maxLocals, 9);
        method.maxStack = Math.max(method.maxStack, 9);
        return changed;
    }

    private static boolean hasTraceBeforeReturn(MethodNode method, AbstractInsnNode returnInstruction, String name) {
        for (AbstractInsnNode instruction = returnInstruction.getPrevious();
                instruction != null && instruction.getOpcode() < 0 || instruction != null;
                instruction = instruction.getPrevious()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && HELPER.equals(call.owner)
                    && name.equals(call.name)) {
                return true;
            }
            if (instruction.getOpcode() >= 0) {
                // The trace is inserted directly before the return, after the previous source operation.
                if (!(instruction instanceof MethodInsnNode) && !(instruction instanceof VarInsnNode)
                        && !(instruction instanceof InsnNode) && !(instruction instanceof JumpInsnNode)) {
                    break;
                }
            }
        }
        return false;
    }

    private static boolean hasAnyCall(MethodNode method, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && HELPER.equals(call.owner) && name.equals(call.name)) return true;
        }
        return false;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name) || !descriptor.equals(method.desc)) continue;
            if (found != null) throw new IllegalStateException("Duplicate AlienEvo diagnostic target "
                    + node.name + "." + name + descriptor);
            found = method;
        }
        return found;
    }
}
