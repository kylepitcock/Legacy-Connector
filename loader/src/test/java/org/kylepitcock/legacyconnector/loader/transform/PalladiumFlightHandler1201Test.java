package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;


/** Regression tests for Palladium FlightHandler's 1.21.1 access and Survival takeoff behavior. */
class PalladiumFlightHandler1201Test implements Opcodes {
    private static ClassNode heightFixture(String fieldName) {
        ClassNode node = new ClassNode();
        node.name = PalladiumFlightHandler1201.FLIGHT_HANDLER;
        MethodNode method = new MethodNode(ACC_PRIVATE, "readHeight", "()F", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(
                GETFIELD, PalladiumFlightHandler1201.ENTITY_DIMENSIONS, fieldName, "F"));
        method.instructions.add(new InsnNode(FRETURN));
        node.methods.add(method);
        return node;
    }

    private static ClassNode takeoffFixture() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumFlightHandler1201.FLIGHT_HANDLER;
        node.superName = "java/lang/Object";
        node.fields.add(new FieldNode(ACC_PRIVATE, "flightType",
                "L" + PalladiumFlightHandler1201.FLIGHT_TYPE + ";", null, null));
        node.fields.add(new FieldNode(ACC_PRIVATE, "player",
                "L" + PalladiumFlightHandler1201.PLAYER + ";", null, null));

        MethodNode setter = new MethodNode(ACC_PUBLIC, "setFlightType",
                "(L" + PalladiumFlightHandler1201.FLIGHT_TYPE + ";)V", null, null);
        setter.instructions.add(new VarInsnNode(ALOAD, 0));
        setter.instructions.add(new VarInsnNode(ALOAD, 1));
        setter.instructions.add(new FieldInsnNode(PUTFIELD, node.name, "flightType",
                "L" + PalladiumFlightHandler1201.FLIGHT_TYPE + ";"));
        setter.instructions.add(new InsnNode(RETURN));
        setter.maxLocals = 2;
        setter.maxStack = 2;
        node.methods.add(setter);

        MethodNode tick = new MethodNode(ACC_PUBLIC, "tick", "()V", null, null);
        LabelNode notFlying = new LabelNode();
        LabelNode reset = new LabelNode();
        tick.instructions.add(new VarInsnNode(ALOAD, 0));
        tick.instructions.add(new FieldInsnNode(GETFIELD, node.name, "flightType",
                "L" + PalladiumFlightHandler1201.FLIGHT_TYPE + ";"));
        tick.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, PalladiumFlightHandler1201.FLIGHT_TYPE,
                "isNotNull", "()Z", false));
        tick.instructions.add(new JumpInsnNode(IFEQ, notFlying));
        tick.instructions.add(new VarInsnNode(ALOAD, 0));
        tick.instructions.add(new FieldInsnNode(GETFIELD, node.name, "player",
                "L" + PalladiumFlightHandler1201.PLAYER + ";"));
        tick.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, PalladiumFlightHandler1201.PLAYER,
                "onGround", "()Z", false));
        tick.instructions.add(new JumpInsnNode(IFNE, reset));
        tick.instructions.add(new VarInsnNode(ALOAD, 0));
        tick.instructions.add(new FieldInsnNode(GETFIELD, node.name, "player",
                "L" + PalladiumFlightHandler1201.PLAYER + ";"));
        tick.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, PalladiumFlightHandler1201.PLAYER,
                "isFallFlying", "()Z", false));
        tick.instructions.add(new JumpInsnNode(IFNE, reset));
        tick.instructions.add(notFlying);
        tick.instructions.add(new InsnNode(RETURN));
        tick.instructions.add(reset);
        tick.instructions.add(new InsnNode(RETURN));
        tick.maxLocals = 1;
        tick.maxStack = 2;
        node.methods.add(tick);

        MethodNode updateMovement = new MethodNode(ACC_PRIVATE, "updateMovement", "()V", null, null);
        updateMovement.instructions.add(new InsnNode(RETURN));
        updateMovement.maxLocals = 1;
        node.methods.add(updateMovement);
        return node;
    }

    private static MethodNode method(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }

    private static boolean hasFieldAccess(MethodNode method, int opcode) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == opcode
                    && PalladiumFlightHandler1201.GROUND_TAKEOFF_GRACE.equals(field.name)) return true;
        }
        return false;
    }

    private static boolean hasIntInstruction(MethodNode method, int opcode, int value) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof IntInsnNode operand
                    && operand.getOpcode() == opcode && operand.operand == value) return true;
        }
        return false;
    }

    private static boolean hasJump(MethodNode method, int opcode) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof JumpInsnNode jump && jump.getOpcode() == opcode) return true;
        }
        return false;
    }

    @Test
    void migratesNamedHeightFieldReadToPublicRecordAccessor() {
        ClassNode node = heightFixture("height");
        assertEquals(1, PalladiumFlightHandler1201.apply(node));
        MethodInsnNode call = assertInstanceOf(
                MethodInsnNode.class, node.methods.get(0).instructions.get(1));
        assertEquals(INVOKEVIRTUAL, call.getOpcode());
        assertEquals(PalladiumFlightHandler1201.ENTITY_DIMENSIONS, call.owner);
        assertEquals("height", call.name);
        assertEquals("()F", call.desc);
    }

    @Test
    void migratesOriginalSrgHeightFieldRead() {
        ClassNode node = heightFixture("f_20378_");
        assertEquals(1, PalladiumFlightHandler1201.apply(node));
        MethodInsnNode call = assertInstanceOf(
                MethodInsnNode.class, node.methods.get(0).instructions.get(1));
        assertEquals("height", call.name);
        assertEquals("()F", call.desc);
    }

    @Test
    void leavesOtherFieldsAndOtherClassesUntouched() {
        ClassNode node = heightFixture("eyeHeight");
        assertEquals(0, PalladiumFlightHandler1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));

        node.name = "net/threetag/palladium/entity/UnrelatedHandler";
        assertEquals(0, PalladiumFlightHandler1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));
        assertEquals(0, PalladiumFlightHandler1201.applyGroundTakeoffGrace(node));
    }

    @Test
    void allowsGroundedActivationToApplyLiftThenRestoresOriginalLandingCancellation() {
        ClassNode node = takeoffFixture();

        assertEquals(1, PalladiumFlightHandler1201.applyGroundTakeoffGrace(node));
        FieldNode grace = node.fields.stream()
                .filter(field -> PalladiumFlightHandler1201.GROUND_TAKEOFF_GRACE.equals(field.name))
                .findFirst().orElseThrow();
        assertEquals(ACC_PRIVATE | ACC_SYNTHETIC, grace.access);
        assertEquals("I", grace.desc);

        MethodNode setter = method(node, "setFlightType",
                "(L" + PalladiumFlightHandler1201.FLIGHT_TYPE + ";)V");
        assertTrue(hasFieldAccess(setter, PUTFIELD));
        assertTrue(hasIntInstruction(setter, BIPUSH,
                PalladiumFlightHandler1201.GROUND_TAKEOFF_GRACE_TICKS));

        MethodNode tick = method(node, "tick", "()V");
        assertTrue(hasFieldAccess(tick, GETFIELD));
        assertTrue(hasFieldAccess(tick, PUTFIELD));
        assertTrue(hasJump(tick, IFLE),
                "the original grounded cancellation branch must run after grace expires");
        assertTrue(hasJump(tick, IFEQ),
                "airborne players must skip the ground grace and clear it");
        assertEquals(0, PalladiumFlightHandler1201.applyGroundTakeoffGrace(node),
                "the adapter must be idempotent");
        assertDoesNotThrow(() -> {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            writer.toByteArray();
        });
    }


    private static boolean hasHelperCall(MethodNode method, String helperName) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && call.owner.equals("legacyconnector/v1_20_1/PalladiumFlightDebugCompat")
                    && call.name.equals(helperName)) return true;
        }
        return false;
    }

    @Test
    void addsOptInTracingAtFlightActivationTickAndMovementStages() {
        ClassNode node = takeoffFixture();

        assertEquals(3, PalladiumFlightHandler1201.applyDiagnostics(node));
        assertTrue(hasHelperCall(method(node, "setFlightType",
                "(L" + PalladiumFlightHandler1201.FLIGHT_TYPE + ";)V"), "logFlightType"));
        assertTrue(hasHelperCall(method(node, "tick", "()V"), "logFlightState"));
        assertTrue(hasHelperCall(method(node, "updateMovement", "()V"), "logFlightState"));

        assertEquals(0, PalladiumFlightHandler1201.applyDiagnostics(node),
                "diagnostic injection must be idempotent");
    }

    @Test
    void rejectsUnexpectedGroundCheckShapeInsteadOfSilentlySkippingTheFix() {
        ClassNode node = takeoffFixture();
        MethodNode tick = method(node, "tick", "()V");
        for (AbstractInsnNode instruction : tick.instructions.toArray()) {
            if (instruction instanceof JumpInsnNode jump && jump.getOpcode() == IFNE) {
                jump.setOpcode(IFEQ);
                break;
            }
        }

        assertThrows(IllegalStateException.class, () ->
                PalladiumFlightHandler1201.applyGroundTakeoffGrace(node));
        assertFalse(node.fields.stream().anyMatch(field ->
                PalladiumFlightHandler1201.GROUND_TAKEOFF_GRACE.equals(field.name)),
                "unexpected bytecode must be rejected before adding the synthetic field");
    }
}
