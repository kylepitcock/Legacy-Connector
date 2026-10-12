package org.kylepitcock.mccompat.v1_7_10.client;

import org.lwjgl.glfw.GLFW;

/** LWJGL 2 keyboard codes (DirectInput scancodes) <-> GLFW key codes. */
public final class KeyCodes {
    private static final int[] TO_GLFW = new int[256];
    private static final java.util.Map<Integer, Integer> FROM_GLFW = new java.util.HashMap<>();

    private static void map(int lwjgl2, int glfw) {
        TO_GLFW[lwjgl2] = glfw;
        FROM_GLFW.put(glfw, lwjgl2);
    }

    static {
        map(1, GLFW.GLFW_KEY_ESCAPE);
        for (int i = 0; i < 9; i++) map(2 + i, GLFW.GLFW_KEY_1 + i);
        map(11, GLFW.GLFW_KEY_0);
        map(12, GLFW.GLFW_KEY_MINUS);
        map(13, GLFW.GLFW_KEY_EQUAL);
        map(14, GLFW.GLFW_KEY_BACKSPACE);
        map(15, GLFW.GLFW_KEY_TAB);
        String row1 = "QWERTYUIOP";
        for (int i = 0; i < row1.length(); i++) map(16 + i, row1.charAt(i));
        map(26, GLFW.GLFW_KEY_LEFT_BRACKET);
        map(27, GLFW.GLFW_KEY_RIGHT_BRACKET);
        map(28, GLFW.GLFW_KEY_ENTER);
        map(29, GLFW.GLFW_KEY_LEFT_CONTROL);
        String row2 = "ASDFGHJKL";
        for (int i = 0; i < row2.length(); i++) map(30 + i, row2.charAt(i));
        map(39, GLFW.GLFW_KEY_SEMICOLON);
        map(40, GLFW.GLFW_KEY_APOSTROPHE);
        map(41, GLFW.GLFW_KEY_GRAVE_ACCENT);
        map(42, GLFW.GLFW_KEY_LEFT_SHIFT);
        map(43, GLFW.GLFW_KEY_BACKSLASH);
        String row3 = "ZXCVBNM";
        for (int i = 0; i < row3.length(); i++) map(44 + i, row3.charAt(i));
        map(51, GLFW.GLFW_KEY_COMMA);
        map(52, GLFW.GLFW_KEY_PERIOD);
        map(53, GLFW.GLFW_KEY_SLASH);
        map(54, GLFW.GLFW_KEY_RIGHT_SHIFT);
        map(55, GLFW.GLFW_KEY_KP_MULTIPLY);
        map(56, GLFW.GLFW_KEY_LEFT_ALT);
        map(57, GLFW.GLFW_KEY_SPACE);
        map(58, GLFW.GLFW_KEY_CAPS_LOCK);
        for (int i = 0; i < 10; i++) map(59 + i, GLFW.GLFW_KEY_F1 + i);
        map(69, GLFW.GLFW_KEY_NUM_LOCK);
        map(70, GLFW.GLFW_KEY_SCROLL_LOCK);
        map(71, GLFW.GLFW_KEY_KP_7);
        map(72, GLFW.GLFW_KEY_KP_8);
        map(73, GLFW.GLFW_KEY_KP_9);
        map(74, GLFW.GLFW_KEY_KP_SUBTRACT);
        map(75, GLFW.GLFW_KEY_KP_4);
        map(76, GLFW.GLFW_KEY_KP_5);
        map(77, GLFW.GLFW_KEY_KP_6);
        map(78, GLFW.GLFW_KEY_KP_ADD);
        map(79, GLFW.GLFW_KEY_KP_1);
        map(80, GLFW.GLFW_KEY_KP_2);
        map(81, GLFW.GLFW_KEY_KP_3);
        map(82, GLFW.GLFW_KEY_KP_0);
        map(83, GLFW.GLFW_KEY_KP_DECIMAL);
        map(87, GLFW.GLFW_KEY_F11);
        map(88, GLFW.GLFW_KEY_F12);
        map(156, GLFW.GLFW_KEY_KP_ENTER);
        map(157, GLFW.GLFW_KEY_RIGHT_CONTROL);
        map(181, GLFW.GLFW_KEY_KP_DIVIDE);
        map(184, GLFW.GLFW_KEY_RIGHT_ALT);
        map(199, GLFW.GLFW_KEY_HOME);
        map(200, GLFW.GLFW_KEY_UP);
        map(201, GLFW.GLFW_KEY_PAGE_UP);
        map(203, GLFW.GLFW_KEY_LEFT);
        map(205, GLFW.GLFW_KEY_RIGHT);
        map(207, GLFW.GLFW_KEY_END);
        map(208, GLFW.GLFW_KEY_DOWN);
        map(209, GLFW.GLFW_KEY_PAGE_DOWN);
        map(210, GLFW.GLFW_KEY_INSERT);
        map(211, GLFW.GLFW_KEY_DELETE);
        map(219, GLFW.GLFW_KEY_LEFT_SUPER);
        map(220, GLFW.GLFW_KEY_RIGHT_SUPER);
    }

    private KeyCodes() {}

    public static int toGlfw(int lwjgl2) {
        return lwjgl2 > 0 && lwjgl2 < TO_GLFW.length ? TO_GLFW[lwjgl2] : GLFW.GLFW_KEY_UNKNOWN;
    }

    public static int fromGlfw(int glfw) {
        return FROM_GLFW.getOrDefault(glfw, 0);
    }
}
