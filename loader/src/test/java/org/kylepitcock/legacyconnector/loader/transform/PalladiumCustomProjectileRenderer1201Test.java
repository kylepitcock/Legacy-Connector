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

/** Regression tests for both private EntityDimensions.height reads in the Palladium projectile renderer. */
class PalladiumCustomProjectileRenderer1201Test implements Opcodes {
    private static ClassNode fixture(String firstName, String secondName) {
        ClassNode node = new ClassNode();
        node.name = PalladiumCustomProjectileRenderer1201.RENDERER;
        MethodNode method = new MethodNode(ACC_PUBLIC, "render", "()V", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(
                GETFIELD, PalladiumCustomProjectileRenderer1201.ENTITY_DIMENSIONS, firstName, "F"));
        method.instructions.add(new InsnNode(POP));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(
                GETFIELD, PalladiumCustomProjectileRenderer1201.ENTITY_DIMENSIONS, secondName, "F"));
        method.instructions.add(new InsnNode(POP));
        method.instructions.add(new InsnNode(RETURN));
        node.methods.add(method);
        return node;
    }

    @Test
    void migratesBothNamedHeightReadsAndIsIdempotent() {
        ClassNode node = fixture("height", "height");
        assertEquals(2, PalladiumCustomProjectileRenderer1201.apply(node));
        int accessors = 0;
        for (var instruction : node.methods.get(0).instructions) {
            if (instruction instanceof MethodInsnNode call) {
                assertEquals(INVOKEVIRTUAL, call.getOpcode());
                assertEquals(PalladiumCustomProjectileRenderer1201.ENTITY_DIMENSIONS, call.owner);
                assertEquals("height", call.name);
                assertEquals("()F", call.desc);
                accessors++;
            }
        }
        assertEquals(2, accessors);
        assertEquals(0, PalladiumCustomProjectileRenderer1201.apply(node));
    }

    @Test
    void migratesBothOriginalSrgHeightReads() {
        ClassNode node = fixture("f_20378_", "f_20378_");
        assertEquals(2, PalladiumCustomProjectileRenderer1201.apply(node));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        ClassNode node = fixture("height", "height");
        node.name = "net/threetag/palladium/client/renderer/entity/UnrelatedRenderer";
        assertEquals(0, PalladiumCustomProjectileRenderer1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));
    }
}
