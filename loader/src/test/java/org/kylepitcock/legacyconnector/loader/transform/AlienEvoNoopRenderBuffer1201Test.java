package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for AlienEvo's no-op VertexConsumer implementation. */
class AlienEvoNoopRenderBuffer1201Test implements Opcodes {
    private static ClassNode node(String name) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = name;
        node.superName = "java/lang/Object";
        node.interfaces.add(AlienEvoNoopRenderBuffer1201.VERTEX);
        MethodNode oldPosition = new MethodNode(ACC_PUBLIC, "addVertex",
                "(DDD)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";", null, null);
        oldPosition.instructions.add(new VarInsnNode(ALOAD, 0));
        oldPosition.instructions.add(new InsnNode(ARETURN));
        node.methods.add(oldPosition);
        return node;
    }

    @Test
    void addsNoopFloatVertexMethodAndReturnsTheSameConsumer() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        assertEquals(1, AlienEvoNoopRenderBuffer1201.apply(node));
        MethodNode addVertex = node.methods.stream()
                .filter(method -> method.name.equals("addVertex")
                        && method.desc.equals("(FFF)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";"))
                .findFirst().orElseThrow();
        AbstractInsnNode[] real = java.util.Arrays.stream(addVertex.instructions.toArray())
                .filter(instruction -> instruction.getOpcode() >= 0)
                .toArray(AbstractInsnNode[]::new);
        assertEquals(2, real.length);
        assertEquals(ALOAD, real[0].getOpcode());
        assertEquals(0, ((VarInsnNode) real[0]).var);
        assertEquals(ARETURN, real[1].getOpcode());
        assertEquals(0, AlienEvoNoopRenderBuffer1201.apply(node));
    }

    @Test
    void leavesUnrelatedConsumersUntouched() {
        ClassNode node = node("example/OtherVertexConsumer");
        assertEquals(0, AlienEvoNoopRenderBuffer1201.apply(node));
        assertEquals(1, node.methods.size());
    }

    @Test
    void failsClosedWhenTheTargetLosesItsLegacyPositionMethod() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        node.methods.clear();
        assertThrows(IllegalStateException.class, () -> AlienEvoNoopRenderBuffer1201.apply(node));
    }

    @Test
    void failsClosedWhenTheTargetDoesNotImplementVertexConsumer() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        node.interfaces.clear();
        assertThrows(IllegalStateException.class, () -> AlienEvoNoopRenderBuffer1201.apply(node));
    }
}
