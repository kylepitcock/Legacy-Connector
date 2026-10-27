package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyKeys;
import org.lwjgl.glfw.GLFW;

/**
 * Joins GLFW's separate key and character events into LWJGL 2's single keyboard event. A key that types a character is held back until
 * its character arrives ({@code keyTyped('e', KEY_E)}, so a focused text field takes the letter instead of the screen closing on the
 * inventory key); keys that type nothing are sent at once, and a held-back key whose character never comes is sent on the next event.
 */
final class KeyRouter1122 {
    private int pendingKey;

    /** A key press of the hosting screen. */
    void keyPressed(GuiScreen legacy, int glfwKey, int modifiers) {
        flush(legacy);
        int legacyKey = LegacyKeys.toLegacy(glfwKey);
        if (legacyKey == 0) return;
        boolean typesCharacter = (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) == 0
                && (glfwKey >= GLFW.GLFW_KEY_SPACE && glfwKey <= GLFW.GLFW_KEY_GRAVE_ACCENT || glfwKey >= GLFW.GLFW_KEY_KP_0 && glfwKey <= GLFW.GLFW_KEY_KP_EQUAL);
        if (typesCharacter) pendingKey = legacyKey;
        else legacy.mcc$keyTyped('\0', legacyKey);
    }

    void charTyped(GuiScreen legacy, char c) {
        int key = pendingKey;
        pendingKey = 0;
        legacy.mcc$keyTyped(c, key);
    }

    void keyReleased(GuiScreen legacy, int glfwKey) {
        flush(legacy);
        int legacyKey = LegacyKeys.toLegacy(glfwKey);
        if (legacyKey != 0) legacy.mcc$keyReleased(legacyKey);
    }

    /** Sends a held-back key whose character did not come (called before other events and each tick). */
    void flush(GuiScreen legacy) {
        if (pendingKey == 0) return;
        int key = pendingKey;
        pendingKey = 0;
        legacy.mcc$keyTyped('\0', key);
    }
}
