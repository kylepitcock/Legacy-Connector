package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for AlienEvo's GeckoLib 4.9.x first-person arm animation call. */
class AlienEvoGeckoRenderLayer1201Test implements Opcodes {
    private static ClassNode node(String name, MethodNode method) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = name;
        node.superName = "java/lang/Object";
        node.methods.add(method);
        return node;
    }

    private static MethodNode legacyRenderArmMethod() {
        MethodNode method = new MethodNode(
                ACC_PUBLIC, "renderArmImpl",
                "(L" + AlienEvoGeckoRenderLayer1201.GEO_MODEL + ";"
                        + "L" + PalladiumGeckoRenderLayer1201.GEO_ANIMATABLE + ";J"
                        + "L" + AlienEvoGeckoRenderLayer1201.ANIMATION_STATE + ";)V",
                null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(ALOAD, 2));
        method.instructions.add(new VarInsnNode(LLOAD, 3));
        method.instructions.add(new VarInsnNode(ALOAD, 5));
        method.instructions.add(new MethodInsnNode(
                INVOKEVIRTUAL,
                AlienEvoGeckoRenderLayer1201.GEO_MODEL,
                "handleAnimations",
                AlienEvoGeckoRenderLayer1201.OLD_HANDLE_ANIMATIONS_DESC,
                false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 5;
        method.maxLocals = 6;
        return method;
    }

    @Test
    void passesThePartialTickFromAnimationStateToGeckoModel() {
        MethodNode method = legacyRenderArmMethod();
        ClassNode node = node(AlienEvoGeckoRenderLayer1201.TARGET_CLASS, method);

        assertEquals(1, AlienEvoGeckoRenderLayer1201.apply(node));

        AbstractInsnNode[] instructions = method.instructions.toArray();
        assertEquals(ALOAD, instructions[0].getOpcode());
        assertEquals(ALOAD, instructions[1].getOpcode());
        assertEquals(LLOAD, instructions[2].getOpcode());
        assertEquals(ALOAD, instructions[3].getOpcode());
        assertEquals(DUP, instructions[4].getOpcode());

        assertTrue(instructions[5] instanceof MethodInsnNode);
        MethodInsnNode partialTick = (MethodInsnNode) instructions[5];
        assertEquals(INVOKEVIRTUAL, partialTick.getOpcode());
        assertEquals(AlienEvoGeckoRenderLayer1201.ANIMATION_STATE, partialTick.owner);
        assertEquals("getPartialTick", partialTick.name);
        assertEquals("()F", partialTick.desc);

        assertTrue(instructions[6] instanceof MethodInsnNode);
        MethodInsnNode rewritten = (MethodInsnNode) instructions[6];
        assertEquals(INVOKEVIRTUAL, rewritten.getOpcode());
        assertEquals(AlienEvoGeckoRenderLayer1201.GEO_MODEL, rewritten.owner);
        assertEquals("handleAnimations", rewritten.name);
        assertEquals(AlienEvoGeckoRenderLayer1201.NEW_HANDLE_ANIMATIONS_DESC, rewritten.desc);
        assertEquals(6, method.maxStack);
        assertEquals(RETURN, instructions[7].getOpcode());

        assertEquals(0, AlienEvoGeckoRenderLayer1201.apply(node),
                "migration must be idempotent once the call has the current descriptor");
    }

    @Test
    void failsClosedWhenTheObservedMethodUsesAnUnknownDescriptor() {
        MethodNode method = legacyRenderArmMethod();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && "handleAnimations".equals(call.name)) {
                call.desc = "(L" + PalladiumGeckoRenderLayer1201.GEO_ANIMATABLE
                        + ";JL" + AlienEvoGeckoRenderLayer1201.ANIMATION_STATE + ";I)V";
            }
        }
        ClassNode node = node(AlienEvoGeckoRenderLayer1201.TARGET_CLASS, method);

        assertThrows(IllegalStateException.class, () -> AlienEvoGeckoRenderLayer1201.apply(node));
    }

    @Test
    void doesNotRewriteAnUnrelatedClass() {
        MethodNode method = new MethodNode(ACC_PUBLIC, "renderArmImpl", "()V", null, null);
        method.instructions.add(new InsnNode(RETURN));
        ClassNode node = node("example/UnrelatedRenderLayer", method);

        assertEquals(0, AlienEvoGeckoRenderLayer1201.apply(node));
        assertEquals(RETURN, method.instructions.getFirst().getOpcode());
    }
}
