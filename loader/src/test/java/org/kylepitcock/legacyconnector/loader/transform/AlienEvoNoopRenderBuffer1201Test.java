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
    void addsFloatVertexAndColorMethodsAsNoopAndIsIdempotent() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        assertEquals(3, AlienEvoNoopRenderBuffer1201.apply(node));

        assertNoop(node, "addVertex", "(FFF)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        assertNoop(node, "setColor", "(IIII)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        assertNoop(node, "setUv", "(FF)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        assertEquals(0, AlienEvoNoopRenderBuffer1201.apply(node));
    }

    @Test
    void replacesAnAbstractColorMethodWithTheNoopImplementation() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        MethodNode color = new MethodNode(ACC_PUBLIC | ACC_ABSTRACT, "setColor",
                "(IIII)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";", null, null);
        node.methods.add(color);

        assertEquals(3, AlienEvoNoopRenderBuffer1201.apply(node));
        assertEquals(0, color.access & ACC_ABSTRACT);
        assertNoop(node, "setColor", "(IIII)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        assertEquals(0, AlienEvoNoopRenderBuffer1201.apply(node));
    }

    @Test
    void replacesAnAbstractUvMethodWithTheNoopImplementation() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        MethodNode uv = new MethodNode(ACC_PUBLIC | ACC_ABSTRACT, "setUv",
                "(FF)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";", null, null);
        node.methods.add(uv);

        assertEquals(3, AlienEvoNoopRenderBuffer1201.apply(node));
        assertEquals(0, uv.access & ACC_ABSTRACT);
        assertNoop(node, "setUv", "(FF)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
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

    private static void assertNoop(ClassNode node, String name, String descriptor) {
        MethodNode method = node.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst().orElseThrow();
        AbstractInsnNode[] real = java.util.Arrays.stream(method.instructions.toArray())
                .filter(instruction -> instruction.getOpcode() >= 0)
                .toArray(AbstractInsnNode[]::new);
        assertEquals(2, real.length);
        assertEquals(ALOAD, real[0].getOpcode());
        assertEquals(0, ((VarInsnNode) real[0]).var);
        assertEquals(ARETURN, real[1].getOpcode());
    }
}
