package legacyconnector.v1_7_10.org.lwjgl.input;

import org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes;

/** LWJGL 2 keyboard polling on top of the modern game's window (legacy tooltips test for Shift this way). */
public class Keyboard {
    public static final int KEY_NONE = 0, KEY_ESCAPE = 1, KEY_LCONTROL = 29, KEY_LSHIFT = 42, KEY_RSHIFT = 54, KEY_LMENU = 56,
            KEY_RCONTROL = 157, KEY_RMENU = 184, KEY_SPACE = 57, KEY_RETURN = 28, KEY_TAB = 15, KEY_BACK = 14;

    public static boolean isKeyDown(int key) {
        int glfw = KeyCodes.toGlfw(key);
        if (glfw <= 0) return false;
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) return false;
            return com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow().getWindow(), glfw);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static boolean isCreated() {
        return true;
    }

    public static void enableRepeatEvents(boolean enable) {
    }

    public static boolean areRepeatEventsEnabled() {
        return false;
    }

    public static boolean next() {
        return false;
    }

    public static int getEventKey() {
        return 0;
    }

    public static boolean getEventKeyState() {
        return false;
    }

    public static char getEventCharacter() {
        return '\0';
    }

    public static void poll() {
    }

    public static void create() {
    }

    public static void destroy() {
    }

    public static String getKeyName(int key) {
        int glfw = KeyCodes.toGlfw(key);
        if (glfw <= 0) return "NONE";
        String n = org.lwjgl.glfw.GLFW.glfwGetKeyName(glfw, 0);
        return n != null ? n.toUpperCase(java.util.Locale.ROOT) : "KEY" + key;
    }

    public static int getKeyIndex(String name) {
        for (int i = 1; i < 256; i++) if (getKeyName(i).equalsIgnoreCase(name)) return i;
        return 0;
    }

    public static int getNumKeyboardEvents() {
        return 0;
    }

    public static int getKeyCount() {
        return 256;
    }
}
