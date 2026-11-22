package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.transform.compat.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for AlienEvo's SuperheroFlightController record-accessor migration. */
class AlienEvoSuperheroFlightController1201Test implements Opcodes {
    private static ClassNode fixture(String fieldName) {
        ClassNode node = new ClassNode();
        node.name = AlienEvoSuperheroFlightController1201.CONTROLLER;
        MethodNode method = new MethodNode(ACC_PUBLIC, "updateHitboxScaling", "()F", null, null);
        node.methods.add(method);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD,
                AlienEvoSuperheroFlightController1201.ENTITY_DIMENSIONS, fieldName, "F"));
        method.instructions.add(new InsnNode(FRETURN));
        method.maxStack = 1;
        method.maxLocals = 1;
        return node;
    }

    @Test
    void migratesHeightReadToPublicRecordAccessorAndIsIdempotent() {
        ClassNode node = fixture("height");
        assertEquals(1, AlienEvoSuperheroFlightController1201.apply(node));
        MethodInsnNode accessor = null;
        for (var instruction : node.methods.get(0).instructions) {
            if (instruction instanceof MethodInsnNode call) accessor = call;
        }
        assertInstanceOf(MethodInsnNode.class, accessor);
        assertEquals(INVOKEVIRTUAL, accessor.getOpcode());
        assertEquals(AlienEvoSuperheroFlightController1201.ENTITY_DIMENSIONS, accessor.owner);
        assertEquals("height", accessor.name);
        assertEquals("()F", accessor.desc);
        assertEquals(0, AlienEvoSuperheroFlightController1201.apply(node));
    }

    @Test
    void migratesOriginalSrgHeightRead() {
        ClassNode node = fixture("f_20378_");
        assertEquals(1, AlienEvoSuperheroFlightController1201.apply(node));
    }

    @Test
    void leavesUnrelatedControllersUntouched() {
        ClassNode node = fixture("height");
        node.name = "example/OtherFlightController";
        assertEquals(0, AlienEvoSuperheroFlightController1201.apply(node));
        assertInstanceOf(FieldInsnNode.class, node.methods.get(0).instructions.get(1));
    }

    @Test
    void failsClosedWhenExpectedHeightReadIsAbsent() {
        ClassNode node = fixture("width");
        assertThrows(IllegalStateException.class, () -> AlienEvoSuperheroFlightController1201.apply(node));
    }
}
