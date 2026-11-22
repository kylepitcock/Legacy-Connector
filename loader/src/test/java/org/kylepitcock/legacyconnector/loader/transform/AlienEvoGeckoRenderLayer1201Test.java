package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.transform.compat.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for all three AlienEvo GeckoLib 4.9.x renderer call sites. */
class AlienEvoGeckoRenderLayer1201Test implements Opcodes {
    private static ClassNode node(String name, MethodNode... methods) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = name;
        node.superName = "java/lang/Object";
        for (MethodNode method : methods) node.methods.add(method);
        return node;
    }

    private static MethodNode legacyRenderMethod(String name) {
        MethodNode method = new MethodNode(
                ACC_PUBLIC, name,
                "(L" + AlienEvoGeckoRenderLayer1201.GEO_MODEL + ";"
                        + "L" + PalladiumGeckoRenderLayer1201.GEO_ANIMATABLE + ";J"
                        + "L" + AlienEvoGeckoRenderLayer1201.ANIMATION_STATE + ";)V",
                null, null);
        // Instance local 0 is this; parameters are GeoModel, GeoAnimatable,
        // long (two slots), then AnimationState.
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

    private static void assertAdapted(MethodNode method) {
        AbstractInsnNode[] instructions = method.instructions.toArray();
        assertEquals(ALOAD, instructions[0].getOpcode());
        assertEquals(ALOAD, instructions[1].getOpcode());
        assertEquals(LLOAD, instructions[2].getOpcode());
        assertEquals(ALOAD, instructions[3].getOpcode());
        assertEquals(DUP, instructions[4].getOpcode());

        if (!(instructions[5] instanceof MethodInsnNode partialTick)) {
            throw new AssertionError("Expected AnimationState.getPartialTick invocation");
        }
        assertEquals(INVOKEVIRTUAL, partialTick.getOpcode());
        assertEquals(AlienEvoGeckoRenderLayer1201.ANIMATION_STATE, partialTick.owner);
        assertEquals("getPartialTick", partialTick.name);
        assertEquals("()F", partialTick.desc);

        if (!(instructions[6] instanceof MethodInsnNode rewritten)) {
            throw new AssertionError("Expected rewritten GeoModel.handleAnimations invocation");
        }
        assertEquals(INVOKEVIRTUAL, rewritten.getOpcode());
        assertEquals(AlienEvoGeckoRenderLayer1201.GEO_MODEL, rewritten.owner);
        assertEquals("handleAnimations", rewritten.name);
        assertEquals(AlienEvoGeckoRenderLayer1201.NEW_HANDLE_ANIMATIONS_DESC, rewritten.desc);
        assertEquals(6, method.maxStack);
        assertEquals(RETURN, instructions[7].getOpcode());
    }

    @Test
    void adaptsAllThreeCallsAndUsesEachAnimationsPartialTick() {
        MethodNode render = legacyRenderMethod("render");
        MethodNode renderArmImpl = legacyRenderMethod("renderArmImpl");
        MethodNode renderBoneSubtree = legacyRenderMethod("renderBoneSubtree");
        ClassNode node = node(
                AlienEvoGeckoRenderLayer1201.TARGET_CLASS,
                render, renderArmImpl, renderBoneSubtree);

        assertEquals(AlienEvoGeckoRenderLayer1201.EXPECTED_CALL_SITES,
                AlienEvoGeckoRenderLayer1201.apply(node));
        assertAdapted(render);
        assertAdapted(renderArmImpl);
        assertAdapted(renderBoneSubtree);

        assertEquals(0, AlienEvoGeckoRenderLayer1201.apply(node),
                "migration must be idempotent once all three calls have the current descriptor");
    }

    @Test
    void failsClosedWhenAnyCallUsesAnUnknownDescriptor() {
        MethodNode render = legacyRenderMethod("render");
        MethodNode renderArmImpl = legacyRenderMethod("renderArmImpl");
        MethodNode renderBoneSubtree = legacyRenderMethod("renderBoneSubtree");
        for (AbstractInsnNode instruction : renderArmImpl.instructions) {
            if (instruction instanceof MethodInsnNode call && "handleAnimations".equals(call.name)) {
                call.desc = "(L" + PalladiumGeckoRenderLayer1201.GEO_ANIMATABLE
                        + ";JL" + AlienEvoGeckoRenderLayer1201.ANIMATION_STATE + ";I)V";
            }
        }
        ClassNode node = node(AlienEvoGeckoRenderLayer1201.TARGET_CLASS, render, renderArmImpl, renderBoneSubtree);

        assertThrows(IllegalStateException.class, () -> AlienEvoGeckoRenderLayer1201.apply(node));
    }

    @Test
    void failsClosedWhenTheInspectedClassHasAnUnexpectedNumberOfCallSites() {
        ClassNode node = node(
                AlienEvoGeckoRenderLayer1201.TARGET_CLASS,
                legacyRenderMethod("render"),
                legacyRenderMethod("renderArmImpl"));

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
