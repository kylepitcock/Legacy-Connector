package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.transform.compat.*;
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
    private static String vertex(String descriptor) {
        return descriptor.replace("LVertexConsumer;", "L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
    }

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
    void suppliesEveryAbstract1211VertexConsumerMutatorAsANoopAndIsIdempotent() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        assertEquals(6, AlienEvoNoopRenderBuffer1201.apply(node));

        assertNoop(node, "addVertex", vertex("(FFF)LVertexConsumer;"));
        assertNoop(node, "setColor", vertex("(IIII)LVertexConsumer;"));
        assertNoop(node, "setUv", vertex("(FF)LVertexConsumer;"));
        assertNoop(node, "setUv1", vertex("(II)LVertexConsumer;"));
        assertNoop(node, "setUv2", vertex("(II)LVertexConsumer;"));
        assertNoop(node, "setNormal", vertex("(FFF)LVertexConsumer;"));
        assertEquals(0, AlienEvoNoopRenderBuffer1201.apply(node));
    }

    @Test
    void replacesAnAbstractColorMethodWithTheNoopImplementation() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        MethodNode color = abstractMethod("setColor", "(IIII)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        node.methods.add(color);

        assertEquals(6, AlienEvoNoopRenderBuffer1201.apply(node));
        assertEquals(0, color.access & ACC_ABSTRACT);
        assertNoop(node, "setColor", "(IIII)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        assertEquals(0, AlienEvoNoopRenderBuffer1201.apply(node));
    }

    @Test
    void replacesTheAbstractOverlayUvMethodThatSetOverlayDelegatesTo() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        MethodNode uv1 = abstractMethod("setUv1", "(II)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        node.methods.add(uv1);

        assertEquals(6, AlienEvoNoopRenderBuffer1201.apply(node));
        assertEquals(0, uv1.access & ACC_ABSTRACT);
        assertNoop(node, "setUv1", "(II)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        assertEquals(0, AlienEvoNoopRenderBuffer1201.apply(node));
    }

    @Test
    void replacesAbstractLightUvAndNormalMethods() {
        ClassNode node = node(AlienEvoNoopRenderBuffer1201.TARGET_CLASS);
        MethodNode uv2 = abstractMethod("setUv2", "(II)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        MethodNode normal = abstractMethod("setNormal", "(FFF)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        node.methods.add(uv2);
        node.methods.add(normal);

        assertEquals(6, AlienEvoNoopRenderBuffer1201.apply(node));
        assertEquals(0, uv2.access & ACC_ABSTRACT);
        assertEquals(0, normal.access & ACC_ABSTRACT);
        assertNoop(node, "setUv2", "(II)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
        assertNoop(node, "setNormal", "(FFF)L" + AlienEvoNoopRenderBuffer1201.VERTEX + ";");
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

    private static MethodNode abstractMethod(String name, String descriptor) {
        return new MethodNode(ACC_PUBLIC | ACC_ABSTRACT, name, descriptor, null, null);
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
