package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Palladium's PowersScreen calls Screen.render() after drawing its own content.
 * Minecraft 1.21.1's Screen render path invokes the modern four-argument
 * renderBackground hook, which applies the in-world blur. Keep the legacy
 * screen's own frame/content rendering but suppress that inherited background.
 */
final class PalladiumPowersScreen1201 implements Opcodes {
    static final String POWERS_SCREEN =
            "net/threetag/palladium/client/screen/power/PowersScreen";

    private static final String SCREEN =
            "net/minecraft/client/gui/screens/Screen";
    private static final String RENDER_BACKGROUND = "renderBackground";
    private static final String GUI_GRAPHICS =
            "net/minecraft/client/gui/GuiGraphics";
    private static final String MODERN_BACKGROUND_DESC =
            "(L" + GUI_GRAPHICS + ";IIF)V";

    private PalladiumPowersScreen1201() {
    }

    /**
     * Supplies the empty modern background hook without changing the rest of
     * PowersScreen.render(), including its Screen.render() widget pass.
     *
     * @return one if the hook was added, zero if it was already present or the class is unrelated
     */
    static int apply(ClassNode node) {
        if (!POWERS_SCREEN.equals(node.name)) {
            return 0;
        }
        if (!SCREEN.equals(node.superName)) {
            throw new IllegalStateException(
                    "Palladium PowersScreen no longer extends the expected Minecraft Screen");
        }

        for (MethodNode method : node.methods) {
            if (!RENDER_BACKGROUND.equals(method.name)
                    || !MODERN_BACKGROUND_DESC.equals(method.desc)) {
                continue;
            }
            int visibility = method.access & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
            if (visibility == ACC_PUBLIC
                    && (method.access & (ACC_ABSTRACT | ACC_STATIC)) == 0
                    && method.instructions != null
                    && method.instructions.size() == 1
                    && method.instructions.getFirst().getOpcode() == RETURN) {
                return 0; // Already adapted.
            }
            throw new IllegalStateException(
                    "Palladium PowersScreen has an unexpected modern renderBackground() override");
        }

        MethodNode background = new MethodNode(
                ACC_PUBLIC, RENDER_BACKGROUND, MODERN_BACKGROUND_DESC, null, null);
        background.instructions.add(new InsnNode(RETURN));
        background.maxStack = 0;
        background.maxLocals = 5; // this, GuiGraphics, mouseX, mouseY, partialTick
        node.methods.add(background);
        return 1;
    }
}
