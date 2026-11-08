package org.kylepitcock.legacyconnector.vb1_7_3.client;

import org.lwjgl.glfw.GLFW;

/**
 * GLFW key codes as LWJGL 2 keyboard codes, which b1.7.3 screens get in {@code keyTyped(char, int)}. LWJGL 2 sent the code and the
 * character in one event; GLFW sends a key event and then a character event, so keys that type a character reach the legacy screen with
 * the character event ({@link #types}).
 */
final class KeysB173 {
    private static final int[] LETTERS = {30, 48, 46, 32, 18, 33, 34, 35, 23, 36, 37, 38, 50, 49, 24, 25, 16, 19, 31, 20, 22, 47, 17, 45, 21, 44};

    private KeysB173() {}

    static int lwjgl(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return LETTERS[key - GLFW.GLFW_KEY_A];
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) return 2 + key - GLFW.GLFW_KEY_1;
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F10) return 59 + key - GLFW.GLFW_KEY_F1;
        return switch (key) {
            case GLFW.GLFW_KEY_0 -> 11;
            case GLFW.GLFW_KEY_ESCAPE -> 1;
            case GLFW.GLFW_KEY_MINUS -> 12;
            case GLFW.GLFW_KEY_EQUAL -> 13;
            case GLFW.GLFW_KEY_BACKSPACE -> 14;
            case GLFW.GLFW_KEY_TAB -> 15;
            case GLFW.GLFW_KEY_LEFT_BRACKET -> 26;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> 27;
            case GLFW.GLFW_KEY_ENTER -> 28;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> 29;
            case GLFW.GLFW_KEY_SEMICOLON -> 39;
            case GLFW.GLFW_KEY_APOSTROPHE -> 40;
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> 41;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> 42;
            case GLFW.GLFW_KEY_BACKSLASH -> 43;
            case GLFW.GLFW_KEY_COMMA -> 51;
            case GLFW.GLFW_KEY_PERIOD -> 52;
            case GLFW.GLFW_KEY_SLASH -> 53;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> 54;
            case GLFW.GLFW_KEY_KP_MULTIPLY -> 55;
            case GLFW.GLFW_KEY_LEFT_ALT -> 56;
            case GLFW.GLFW_KEY_SPACE -> 57;
            case GLFW.GLFW_KEY_CAPS_LOCK -> 58;
            case GLFW.GLFW_KEY_F11 -> 87;
            case GLFW.GLFW_KEY_F12 -> 88;
            case GLFW.GLFW_KEY_KP_ENTER -> 156;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> 157;
            case GLFW.GLFW_KEY_RIGHT_ALT -> 184;
            case GLFW.GLFW_KEY_HOME -> 199;
            case GLFW.GLFW_KEY_UP -> 200;
            case GLFW.GLFW_KEY_PAGE_UP -> 201;
            case GLFW.GLFW_KEY_LEFT -> 203;
            case GLFW.GLFW_KEY_RIGHT -> 205;
            case GLFW.GLFW_KEY_END -> 207;
            case GLFW.GLFW_KEY_DOWN -> 208;
            case GLFW.GLFW_KEY_PAGE_DOWN -> 209;
            case GLFW.GLFW_KEY_INSERT -> 210;
            case GLFW.GLFW_KEY_DELETE -> 211;
            default -> 0;
        };
    }

    /** The character LWJGL 2 reported with keys that type none of their own (Backspace 8, Enter 13, Escape 27, Tab 9). */
    static char character(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> '\b';
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> '\r';
            case GLFW.GLFW_KEY_ESCAPE -> (char) 27;
            case GLFW.GLFW_KEY_TAB -> '\t';
            default -> (char) 0;
        };
    }

    /** True if the key will also send a character event (printable, no Control or Alt held). */
    static boolean types(int key, int modifiers) {
        return key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_GRAVE_ACCENT && (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT)) == 0;
    }
}
