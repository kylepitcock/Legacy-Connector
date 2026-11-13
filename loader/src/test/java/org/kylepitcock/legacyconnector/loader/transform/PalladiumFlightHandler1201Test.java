package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for Palladium's private EntityDimensions.height field access on 1.21.1. */
class PalladiumFlightHandler1201Test implements Opcodes {
    private static ClassNode fixture(String fieldName) {
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

    @Test
    void migratesNamedHeightFieldReadToPublicRecordAccessor() {
        ClassNode node = fixture("height");
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
        ClassNode node = fixture("f_20378_");
        assertEquals(1, PalladiumFlightHandler1201.apply(node));
        MethodInsnNode call = assertInstanceOf(
                MethodInsnNode.class, node.methods.get(0).instructions.get(1));
        assertEquals("height", call.name);
        assertEquals("()F", call.desc);
    }

    @Test
    void leavesOtherFieldsAndOtherClassesUntouched() {
        ClassNode node = fixture("eyeHeight");
        assertEquals(0, PalladiumFlightHandler1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));

        node.name = "net/threetag/palladium/entity/UnrelatedHandler";
        assertEquals(0, PalladiumFlightHandler1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));
    }
}
