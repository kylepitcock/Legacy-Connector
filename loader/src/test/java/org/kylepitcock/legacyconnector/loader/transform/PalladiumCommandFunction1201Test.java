package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for Palladium's five command-function execution call sites. */
class PalladiumCommandFunction1201Test implements Opcodes {
    private static ClassNode node(String name, MethodNode... methods) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = name;
        node.superName = "java/lang/Object";
        node.methods.addAll(List.of(methods));
        return node;
    }

    private static MethodNode callMethod(String name, int calls) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, name, "()V", null, null);
        for (int i = 0; i < calls; i++) {
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            method.instructions.add(new VarInsnNode(ALOAD, 1));
            method.instructions.add(new VarInsnNode(ALOAD, 2));
            method.instructions.add(new MethodInsnNode(
                    INVOKEVIRTUAL,
                    PalladiumCommandFunction1201.SERVER_FUNCTION_MANAGER,
                    "execute",
                    PalladiumCommandFunction1201.OLD_DESC,
                    false));
            method.instructions.add(new InsnNode(POP));
        }
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 3;
        method.maxLocals = 3;
        return method;
    }

    private static void assertCallsUseHelper(MethodNode method, int expected) {
        int helperCalls = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && PalladiumCommandFunction1201.HELPER.equals(call.owner)) {
                assertEquals(INVOKESTATIC, call.getOpcode());
                assertEquals("execute", call.name);
                assertEquals(PalladiumCommandFunction1201.HELPER_DESC, call.desc);
                helperCalls++;
            }
            assertFalse(instruction.getOpcode() == POP,
                    "the old execute() integer result should no longer be popped");
        }
        assertEquals(expected, helperCalls);
    }

    @Test
    void migratesAllThreeCommandAbilityPhasesAndIsIdempotent() {
        MethodNode firstTick = callMethod("firstTick", 1);
        MethodNode tick = callMethod("tick", 1);
        MethodNode lastTick = callMethod("lastTick", 1);
        ClassNode node = node(
                PalladiumCommandFunction1201.COMMAND_ABILITY,
                firstTick, tick, lastTick);

        assertEquals(3, PalladiumCommandFunction1201.apply(node));
        assertCallsUseHelper(firstTick, 1);
        assertCallsUseHelper(tick, 1);
        assertCallsUseHelper(lastTick, 1);
        assertEquals(0, PalladiumCommandFunction1201.apply(node));
    }

    @Test
    void migratesBothEnergyBeamHitPathsAndIsIdempotent() {
        MethodNode tick = callMethod("tick", 2);
        ClassNode node = node(PalladiumCommandFunction1201.ENERGY_BEAM_ABILITY, tick);

        assertEquals(2, PalladiumCommandFunction1201.apply(node));
        assertCallsUseHelper(tick, 2);
        assertEquals(0, PalladiumCommandFunction1201.apply(node));
    }

    @Test
    void doesNotTouchUnrelatedClasses() {
        MethodNode tick = callMethod("tick", 1);
        ClassNode node = node("example/UnrelatedAbility", tick);

        assertEquals(0, PalladiumCommandFunction1201.apply(node));
        assertTrue(tick.instructions.iterator().hasNext());
    }
}
