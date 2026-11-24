package org.kylepitcock.legacyconnector.loader.transform;

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

/** Regression coverage for AlienEvo Grunt melee reach on Minecraft 1.21.1. */
class AlienEvoGruntMeleeGoal1201Test implements Opcodes {
    private static ClassNode fixture() {
        ClassNode node = new ClassNode();
        node.name = AlienEvoGruntMeleeGoal1201.TARGET;
        MethodNode reach = new MethodNode(ACC_PROTECTED, "getAttackReachSqr",
                AlienEvoGruntMeleeGoal1201.REACH_DESC, null, null);
        node.methods.add(reach);

        reach.instructions.add(new VarInsnNode(ALOAD, 0));
        reach.instructions.add(new VarInsnNode(ALOAD, 1));
        reach.instructions.add(new MethodInsnNode(INVOKESPECIAL, AlienEvoGruntMeleeGoal1201.SUPER,
                "getAttackReachSqr", AlienEvoGruntMeleeGoal1201.REACH_DESC, false));
        reach.instructions.add(new VarInsnNode(ALOAD, 0));
        reach.instructions.add(new FieldInsnNode(GETFIELD, AlienEvoGruntMeleeGoal1201.TARGET,
                "this$0", "L" + AlienEvoGruntMeleeGoal1201.GRUNT + ";"));
        reach.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, AlienEvoGruntMeleeGoal1201.GRUNT,
                "extraReachSqr", "()D", false));
        reach.instructions.add(new InsnNode(DADD));
        reach.instructions.add(new InsnNode(DRETURN));
        reach.maxStack = 2;
        reach.maxLocals = 2;
        return node;
    }

    @Test
    void replacesOnlyTheSuperclassReachCallAndKeepsAlienEvoExtraReach() {
        ClassNode node = fixture();

        assertEquals(1, AlienEvoGruntMeleeGoal1201.apply(node));

        MethodNode method = node.methods.get(0);
        int bridgeCalls = 0;
        int extraReachCalls = 0;
        int legacyCalls = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if ("baseAttackReachSqr".equals(call.name)) {
                    bridgeCalls++;
                    assertEquals(INVOKESTATIC, call.getOpcode());
                    assertEquals(AlienEvoGruntMeleeGoal1201.HELPER, call.owner);
                    assertEquals(AlienEvoGruntMeleeGoal1201.BRIDGE_DESC, call.desc);
                }
                if ("extraReachSqr".equals(call.name)) extraReachCalls++;
                if ("getAttackReachSqr".equals(call.name)
                        && AlienEvoGruntMeleeGoal1201.SUPER.equals(call.owner)) legacyCalls++;
            }
        }
        assertEquals(1, bridgeCalls);
        assertEquals(1, extraReachCalls);
        assertEquals(0, legacyCalls);
        assertEquals(0, AlienEvoGruntMeleeGoal1201.apply(node));
    }

    @Test
    void failsClosedWhenTheExpectedExtraReachTailChanges() {
        ClassNode node = fixture();
        MethodNode method = node.methods.get(0);
        method.instructions.remove(method.instructions.getLast().getPrevious());
        assertThrows(IllegalStateException.class, () -> AlienEvoGruntMeleeGoal1201.apply(node));
    }

    @Test
    void failsClosedWhenTheSuperclassCallHasAnUnexpectedStackShape() {
        ClassNode node = fixture();
        MethodNode method = node.methods.get(0);
        method.instructions.set(method.instructions.getFirst(), new InsnNode(NOP));
        assertThrows(IllegalStateException.class, () -> AlienEvoGruntMeleeGoal1201.apply(node));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        ClassNode node = fixture();
        node.name = "example/OtherGoal";
        assertEquals(0, AlienEvoGruntMeleeGoal1201.apply(node));
        assertInstanceOf(MethodInsnNode.class, node.methods.get(0).instructions.get(2));
    }

    @Test
    void isIntegratedIntoTheAlienEvoTransformerPass() {
        ClassNode node = fixture();
        assertEquals(1, AlienEvoMixins1201.apply(node));
        assertEquals(0, AlienEvoMixins1201.apply(node));
    }
}
