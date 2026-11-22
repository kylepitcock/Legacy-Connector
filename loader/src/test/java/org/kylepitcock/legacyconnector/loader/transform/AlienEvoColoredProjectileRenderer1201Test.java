package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.transform.compat.*;
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

/** Regression tests for AlienEvo's ColoredProjectileRenderer record accessor migration. */
class AlienEvoColoredProjectileRenderer1201Test implements Opcodes {
    private static ClassNode fixture(String fieldName) {
        ClassNode node = new ClassNode();
        node.name = AlienEvoColoredProjectileRenderer1201.RENDERER;
        MethodNode method = new MethodNode(ACC_PUBLIC, "centeringOffset", "()F", null, null);
        node.methods.add(method);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD,
                AlienEvoColoredProjectileRenderer1201.ENTITY_DIMENSIONS, fieldName, "F"));
        method.instructions.add(new InsnNode(FRETURN));
        method.maxStack = 1;
        method.maxLocals = 1;
        return node;
    }

    @Test
    void migratesHeightReadToPublicRecordAccessorAndIsIdempotent() {
        ClassNode node = fixture("height");
        assertEquals(1, AlienEvoColoredProjectileRenderer1201.apply(node));
        MethodInsnNode accessor = null;
        for (var instruction : node.methods.get(0).instructions) {
            if (instruction instanceof MethodInsnNode call) accessor = call;
        }
        assertInstanceOf(MethodInsnNode.class, accessor);
        assertEquals(INVOKEVIRTUAL, accessor.getOpcode());
        assertEquals(AlienEvoColoredProjectileRenderer1201.ENTITY_DIMENSIONS, accessor.owner);
        assertEquals("height", accessor.name);
        assertEquals("()F", accessor.desc);
        assertEquals(0, AlienEvoColoredProjectileRenderer1201.apply(node));
    }

    @Test
    void migratesOriginalSrgHeightRead() {
        ClassNode node = fixture("f_20378_");
        assertEquals(1, AlienEvoColoredProjectileRenderer1201.apply(node));
    }

    @Test
    void leavesUnrelatedRenderersUntouched() {
        ClassNode node = fixture("height");
        node.name = "example/OtherProjectileRenderer";
        assertEquals(0, AlienEvoColoredProjectileRenderer1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));
    }
}
