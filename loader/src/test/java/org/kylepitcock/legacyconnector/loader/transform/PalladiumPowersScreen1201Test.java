package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Regression tests for suppressing blur behind Palladium's PowersScreen. */
class PalladiumPowersScreen1201Test implements Opcodes {
    private static final String GUI_GRAPHICS =
            "net/minecraft/client/gui/GuiGraphics";
    private static final String BACKGROUND_DESC =
            "(L" + GUI_GRAPHICS + ";IIF)V";

    @Test
    void addsAnEmptyModernBackgroundHookAndIsIdempotent() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = PalladiumPowersScreen1201.POWERS_SCREEN;
        node.superName = "net/minecraft/client/gui/screens/Screen";

        assertEquals(1, PalladiumPowersScreen1201.apply(node));

        MethodNode background = null;
        for (MethodNode method : node.methods) {
            if ("renderBackground".equals(method.name) && BACKGROUND_DESC.equals(method.desc)) {
                background = method;
                break;
            }
        }
        assertNotNull(background);
        assertTrue((background.access & ACC_PUBLIC) != 0);
        assertEquals(1, background.instructions.size());
        assertEquals(RETURN, background.instructions.getFirst().getOpcode());
        assertEquals(0, PalladiumPowersScreen1201.apply(node));
    }

    @Test
    void ignoresUnrelatedClasses() {
        ClassNode node = new ClassNode();
        node.name = "example/Unrelated";
        node.superName = "java/lang/Object";
        MethodNode method = new MethodNode(ACC_PUBLIC, "render", "()V", null, null);
        method.instructions.add(new InsnNode(RETURN));
        node.methods.add(method);

        assertEquals(0, PalladiumPowersScreen1201.apply(node));
        assertEquals(1, node.methods.size());
    }
}
