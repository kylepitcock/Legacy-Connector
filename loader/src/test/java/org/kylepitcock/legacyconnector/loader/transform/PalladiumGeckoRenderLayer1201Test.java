package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for Palladium's GeckoLib 4.9.x first-person animation call. */
class PalladiumGeckoRenderLayer1201Test implements Opcodes {
    private static ClassNode node(String name, MethodNode method) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = name;
        node.superName = "java/lang/Object";
        node.methods.add(method);
        return node;
    }

    @Test
    void passesThePartialTickFromAnimationStateToTheNewGeckoModelMethod() {
        MethodNode method = new MethodNode(
                ACC_PUBLIC, "renderArm",
                "(L" + PalladiumGeckoRenderLayer1201.GEO_MODEL + ";"
                        + "L" + PalladiumGeckoRenderLayer1201.GEO_ANIMATABLE + ";J"
                        + "L" + PalladiumGeckoRenderLayer1201.ANIMATION_STATE + ";)V",
                null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(LLOAD, 2));
        method.instructions.add(new VarInsnNode(ALOAD, 4));
        MethodInsnNode oldCall = new MethodInsnNode(
                INVOKEVIRTUAL,
                PalladiumGeckoRenderLayer1201.GEO_MODEL,
                "handleAnimations",
                PalladiumGeckoRenderLayer1201.OLD_HANDLE_ANIMATIONS_DESC,
                false);
        method.instructions.add(oldCall);
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 5;
        method.maxLocals = 5;

        ClassNode node = node(PalladiumGeckoRenderLayer1201.TARGET_CLASS, method);
        assertEquals(1, PalladiumGeckoRenderLayer1201.apply(node));

        AbstractInsnNode[] instructions = method.instructions.toArray();
        assertEquals(ALOAD, instructions[0].getOpcode());
        assertEquals(ALOAD, instructions[1].getOpcode());
        assertEquals(LLOAD, instructions[2].getOpcode());
        assertEquals(ALOAD, instructions[3].getOpcode());
        assertEquals(DUP, instructions[4].getOpcode());

        assertTrue(instructions[5] instanceof MethodInsnNode);
        MethodInsnNode partialTick = (MethodInsnNode) instructions[5];
        assertEquals(INVOKEVIRTUAL, partialTick.getOpcode());
        assertEquals(PalladiumGeckoRenderLayer1201.ANIMATION_STATE, partialTick.owner);
        assertEquals("getPartialTick", partialTick.name);
        assertEquals("()F", partialTick.desc);

        assertTrue(instructions[6] instanceof MethodInsnNode);
        MethodInsnNode rewritten = (MethodInsnNode) instructions[6];
        assertEquals(INVOKEVIRTUAL, rewritten.getOpcode());
        assertEquals(PalladiumGeckoRenderLayer1201.GEO_MODEL, rewritten.owner);
        assertEquals("handleAnimations", rewritten.name);
        assertEquals(PalladiumGeckoRenderLayer1201.NEW_HANDLE_ANIMATIONS_DESC, rewritten.desc);
        assertTrue(method.instructions.contains(oldCall));
        assertEquals(6, method.maxStack);
        assertEquals(RETURN, instructions[7].getOpcode());
    }

    @Test
    void doesNotRewriteTheSameLibraryCallInAnUnrelatedClass() {
        MethodNode method = new MethodNode(ACC_PUBLIC, "renderArm", "()V", null, null);
        method.instructions.add(new InsnNode(RETURN));
        ClassNode node = node("example/UnrelatedRenderLayer", method);

        assertEquals(0, PalladiumGeckoRenderLayer1201.apply(node));
        assertEquals(RETURN, method.instructions.getFirst().getOpcode());
    }
}
