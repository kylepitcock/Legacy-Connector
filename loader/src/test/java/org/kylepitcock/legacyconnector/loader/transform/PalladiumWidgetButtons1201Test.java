package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for Palladium's removed inherited button texture field. */
class PalladiumWidgetButtons1201Test implements Opcodes {
    private static final String RESOURCE_LOCATION =
            "Lnet/minecraft/resources/ResourceLocation;";
    private static final String GUI_GRAPHICS =
            "net/minecraft/client/gui/GuiGraphics";
    private static final String BLIT_HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/MiddleClientCompat";
    private static final String LEGACY_BLIT_DESCRIPTOR =
            "(L" + GUI_GRAPHICS + ";" + RESOURCE_LOCATION + "IIIIIIIIII)V";

    @Test
    void iconButtonUsesGuiSpriteAndKeepsFlatIconTextureReference() {
        ClassNode node = buttonNode(PalladiumWidgetButtons1201.ICON_BUTTON, true);

        assertEquals(1, PalladiumWidgetButtons1201.apply(node));

        MethodNode render = node.methods.get(0);
        assertFalse(hasStaticField(render, node.name, "WIDGETS_LOCATION"),
                "the removed inherited widget texture must not survive");
        assertTrue(hasOpcode(render, ACONST_NULL),
                "the legacy texture operand is replaced by a null sentinel");
        assertTrue(hasStaticField(render,
                "net/threetag/palladium/client/screen/components/FlatIconButton",
                "WIDGETS_LOCATION"),
                "IconButton's separate custom texture path must remain untouched");
        assertTrue(hasCall(render, BLIT_HELPER, "blitPalladiumDefaultButton"),
                "the sentinel must only be consumed by the sprite-aware helper");
        assertFalse(hasCall(render, BLIT_HELPER, "blitNineSliced"),
                "the null sentinel must not reach the raw-texture nine-slice helper");
    }

    @Test
    void textWithIconButtonUsesGuiSprite() {
        ClassNode node = buttonNode(PalladiumWidgetButtons1201.TEXT_WITH_ICON_BUTTON, false);

        assertEquals(1, PalladiumWidgetButtons1201.apply(node));

        MethodNode render = node.methods.get(0);
        assertFalse(hasStaticField(render, node.name, "WIDGETS_LOCATION"));
        assertTrue(hasCall(render, BLIT_HELPER, "blitPalladiumDefaultButton"));
        assertFalse(hasCall(render, BLIT_HELPER, "blitNineSliced"));
    }

    @Test
    void unrelatedClassesAndTextureReferencesAreNotRewritten() {
        ClassNode node = buttonNode("example/OtherButton", false);

        assertEquals(0, PalladiumWidgetButtons1201.apply(node));

        MethodNode render = node.methods.get(0);
        assertTrue(hasStaticField(render, node.name, "WIDGETS_LOCATION"));
        assertTrue(hasCall(render, BLIT_HELPER, "blitNineSliced"));
        assertFalse(hasCall(render, BLIT_HELPER, "blitPalladiumDefaultButton"));
    }

    private static ClassNode buttonNode(String name, boolean includeFlatIconTexture) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = name;
        node.superName = "net/minecraft/client/gui/components/Button";

        MethodNode render = new MethodNode(
                ACC_PUBLIC | ACC_STATIC, "renderWidget", "(L" + GUI_GRAPHICS + ";)V", null, null);
        render.instructions.add(new VarInsnNode(ALOAD, 0));
        if (includeFlatIconTexture) {
            render.instructions.add(new FieldInsnNode(GETSTATIC,
                    "net/threetag/palladium/client/screen/components/FlatIconButton",
                    "WIDGETS_LOCATION", RESOURCE_LOCATION));
            render.instructions.add(new InsnNode(POP));
            render.instructions.add(new VarInsnNode(ALOAD, 0));
        }
        render.instructions.add(new FieldInsnNode(GETSTATIC, name, "WIDGETS_LOCATION", RESOURCE_LOCATION));
        for (int i = 0; i < 10; i++) {
            render.instructions.add(new InsnNode(ICONST_0));
        }
        render.instructions.add(new MethodInsnNode(INVOKESTATIC,
                BLIT_HELPER, "blitNineSliced", LEGACY_BLIT_DESCRIPTOR, false));
        render.instructions.add(new InsnNode(RETURN));
        render.maxStack = 12;
        render.maxLocals = 1;
        node.methods.add(render);
        return node;
    }

    private static boolean hasStaticField(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == GETSTATIC
                    && owner.equals(field.owner)
                    && name.equals(field.name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOpcode(MethodNode method, int opcode) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() == opcode) return true;
        }
        return false;
    }

    private static boolean hasCall(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && owner.equals(call.owner)
                    && name.equals(call.name)) {
                return true;
            }
        }
        return false;
    }
}
