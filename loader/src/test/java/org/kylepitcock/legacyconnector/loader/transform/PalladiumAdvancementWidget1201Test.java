package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Regression tests for Palladium's removed AdvancementWidgetType#getIndex lookup. */
class PalladiumAdvancementWidget1201Test implements Opcodes {
    private static final String ENUM =
            "net/minecraft/client/gui/screens/advancements/AdvancementWidgetType";

    @Test
    void redirectsAllTooltipFrameIndicesToTheUnchangedEnumOrderAndIsIdempotent() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = PalladiumAdvancementWidget1201.TREE_ABILITY_WIDGET;
        node.superName = "java/lang/Object";

        MethodNode drawHover = new MethodNode(
                ACC_PUBLIC,
                "drawHover",
                "(Lnet/minecraft/client/gui/GuiGraphics;IIFII)V",
                null,
                null);
        for (int i = 0; i < 3; i++) {
            drawHover.instructions.add(new InsnNode(ACONST_NULL));
            drawHover.instructions.add(new MethodInsnNode(
                    INVOKEVIRTUAL, ENUM, "getIndex", "()I", false));
            drawHover.instructions.add(new InsnNode(POP));
        }
        drawHover.instructions.add(new InsnNode(RETURN));
        node.methods.add(drawHover);

        assertEquals(3, PalladiumAdvancementWidget1201.apply(node));

        int ordinals = 0;
        int oldCalls = 0;
        for (var instruction : drawHover.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if ("java/lang/Enum".equals(call.owner)
                        && "ordinal".equals(call.name)
                        && "()I".equals(call.desc)
                        && call.getOpcode() == INVOKEVIRTUAL) {
                    ordinals++;
                }
                if (ENUM.equals(call.owner) && "getIndex".equals(call.name)) {
                    oldCalls++;
                }
            }
        }
        assertEquals(3, ordinals);
        assertEquals(0, oldCalls);
        assertEquals(0, PalladiumAdvancementWidget1201.apply(node));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        ClassNode node = new ClassNode();
        node.name = "example/Unrelated";
        node.superName = "java/lang/Object";

        assertEquals(0, PalladiumAdvancementWidget1201.apply(node));
        assertTrue(node.methods.isEmpty());
        assertFalse(PalladiumAdvancementWidget1201.TREE_ABILITY_WIDGET.equals(node.name));
    }
}
