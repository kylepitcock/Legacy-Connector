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

/** Regression tests for AlienEvo's DynamicColorParticleRenderType API migration. */
class AlienEvoDynamicColorParticleRenderType1201Test implements Opcodes {
    private static ClassNode renderType() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = AlienEvoDynamicColorParticleRenderType1201.TARGET;
        node.superName = "java/lang/Object";
        node.interfaces.add("net/minecraft/client/particle/ParticleRenderType");

        MethodNode begin = new MethodNode(ACC_PUBLIC, "begin",
                AlienEvoDynamicColorParticleRenderType1201.LEGACY_BEGIN_DESC, null, null);
        begin.instructions.add(new VarInsnNode(ALOAD, 1));
        begin.instructions.add(new FieldInsnNode(GETSTATIC,
                AlienEvoDynamicColorParticleRenderType1201.VERTEX_MODE, "QUADS",
                "L" + AlienEvoDynamicColorParticleRenderType1201.VERTEX_MODE + ";"));
        begin.instructions.add(new FieldInsnNode(GETSTATIC,
                "com/mojang/blaze3d/vertex/DefaultVertexFormat", "PARTICLE",
                "L" + AlienEvoDynamicColorParticleRenderType1201.VERTEX_FORMAT + ";"));
        begin.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                AlienEvoDynamicColorParticleRenderType1201.COMPAT_BUILDER, "begin",
                AlienEvoDynamicColorParticleRenderType1201.COMPAT_BUILDER_BEGIN_DESC, false));
        begin.instructions.add(new InsnNode(RETURN));
        begin.maxStack = 3;
        begin.maxLocals = 3;
        node.methods.add(begin);
        return node;
    }

    @Test
    void migratesLegacyBufferBuilderContractToTesselatorReturningBufferBuilder() {
        ClassNode node = renderType();

        assertEquals(1, AlienEvoDynamicColorParticleRenderType1201.apply(node));

        MethodNode begin = node.methods.get(0);
        assertEquals(AlienEvoDynamicColorParticleRenderType1201.MODERN_BEGIN_DESC, begin.desc);
        MethodInsnNode tessellatorBegin = null;
        int returns = 0;
        for (var instruction : begin.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call) tessellatorBegin = call;
            if (instruction.getOpcode() == ARETURN) returns++;
        }
        assertInstanceOf(MethodInsnNode.class, tessellatorBegin);
        assertEquals(INVOKEVIRTUAL, tessellatorBegin.getOpcode());
        assertEquals(AlienEvoDynamicColorParticleRenderType1201.TESSELATOR, tessellatorBegin.owner);
        assertEquals("begin", tessellatorBegin.name);
        assertEquals(AlienEvoDynamicColorParticleRenderType1201.TESSELATOR_BEGIN_DESC, tessellatorBegin.desc);
        assertEquals(1, returns);
        assertEquals(0, AlienEvoDynamicColorParticleRenderType1201.apply(node));
    }

    @Test
    void failsClosedWhenTheExpectedBuilderBeginCallChanges() {
        ClassNode node = renderType();
        MethodNode begin = node.methods.get(0);
        for (var instruction : begin.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("begin")) {
                call.desc = "()V";
            }
        }
        assertThrows(IllegalStateException.class, () -> AlienEvoDynamicColorParticleRenderType1201.apply(node));
    }

    @Test
    void failsClosedWhenTheBeginMethodDoesNotReturnTheStartedBuffer() {
        ClassNode node = renderType();
        node.methods.get(0).instructions.clear();
        node.methods.get(0).instructions.add(new InsnNode(RETURN));
        assertThrows(IllegalStateException.class, () -> AlienEvoDynamicColorParticleRenderType1201.apply(node));
    }

    @Test
    void leavesOtherClassesUntouched() {
        ClassNode node = renderType();
        node.name = "example/OtherParticleRenderType";
        assertEquals(0, AlienEvoDynamicColorParticleRenderType1201.apply(node));
    }
}
