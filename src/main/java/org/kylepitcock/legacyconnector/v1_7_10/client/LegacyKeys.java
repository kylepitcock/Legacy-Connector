package org.kylepitcock.legacyconnector.v1_7_10.client;

import org.lwjgl.glfw.GLFW;

/** GLFW key codes (LWJGL 3) to the LWJGL 2 {@code Keyboard.KEY_*} codes legacy GUIs compare against. */
public final class LegacyKeys {
    private static final int[] LETTERS = {30, 48, 46, 32, 18, 33, 34, 35, 23, 36, 37, 38, 50, 49, 24, 25, 16, 19, 31, 20, 22, 47, 17, 45, 21, 44};

    private LegacyKeys() {}

    public static int toLegacy(int glfw) {
        if (glfw >= GLFW.GLFW_KEY_A && glfw <= GLFW.GLFW_KEY_Z) return LETTERS[glfw - GLFW.GLFW_KEY_A];
        if (glfw >= GLFW.GLFW_KEY_1 && glfw <= GLFW.GLFW_KEY_9) return 2 + (glfw - GLFW.GLFW_KEY_1);
        if (glfw >= GLFW.GLFW_KEY_F1 && glfw <= GLFW.GLFW_KEY_F10) return 59 + (glfw - GLFW.GLFW_KEY_F1);
        return switch (glfw) {
            case GLFW.GLFW_KEY_0 -> 11;
            case GLFW.GLFW_KEY_ESCAPE -> 1;
            case GLFW.GLFW_KEY_ENTER -> 28;
            case GLFW.GLFW_KEY_KP_ENTER -> 156;
            case GLFW.GLFW_KEY_BACKSPACE -> 14;
            case GLFW.GLFW_KEY_TAB -> 15;
            case GLFW.GLFW_KEY_SPACE -> 57;
            case GLFW.GLFW_KEY_MINUS -> 12;
            case GLFW.GLFW_KEY_EQUAL -> 13;
            case GLFW.GLFW_KEY_UP -> 200;
            case GLFW.GLFW_KEY_DOWN -> 208;
            case GLFW.GLFW_KEY_LEFT -> 203;
            case GLFW.GLFW_KEY_RIGHT -> 205;
            case GLFW.GLFW_KEY_DELETE -> 211;
            case GLFW.GLFW_KEY_HOME -> 199;
            case GLFW.GLFW_KEY_END -> 207;
            case GLFW.GLFW_KEY_PAGE_UP -> 201;
            case GLFW.GLFW_KEY_PAGE_DOWN -> 209;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> 42;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> 54;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> 29;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> 157;
            case GLFW.GLFW_KEY_LEFT_ALT -> 56;
            case GLFW.GLFW_KEY_F11 -> 87;
            case GLFW.GLFW_KEY_F12 -> 88;
            default -> 0;
        };
    }
}
