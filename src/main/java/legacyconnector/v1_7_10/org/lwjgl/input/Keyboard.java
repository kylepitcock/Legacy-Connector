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

    /** The key event being handled (FML's KeyInputEvent handlers read it), in LWJGL 2 key codes. */
    private static volatile int eventKey;
    private static volatile boolean eventKeyState;
    private static volatile char eventCharacter;

    public static void mcc$setEvent(int key, boolean state, char character) {
        eventKey = key;
        eventKeyState = state;
        eventCharacter = character;
    }

    public static int getEventKey() {
        return eventKey;
    }

    public static boolean getEventKeyState() {
        return eventKeyState;
    }

    public static char getEventCharacter() {
        return eventCharacter;
    }

    public static boolean isRepeatEvent() {
        return false;
    }

    public static long getEventNanoseconds() {
        return System.nanoTime();
    }

    public static void poll() {
    }

    public static void create() {
    }

    public static void destroy() {
    }

    /** LWJGL 2's names of the keys that have no printable character (GLFW only names printable ones). */
    private static final java.util.Map<Integer, String> KEY_NAMES = new java.util.HashMap<>();

    static {
        Object[] names = {1, "ESCAPE", 14, "BACK", 15, "TAB", 28, "RETURN", 29, "LCONTROL", 42, "LSHIFT", 54, "RSHIFT", 56, "LMENU",
                57, "SPACE", 58, "CAPITAL", 59, "F1", 60, "F2", 61, "F3", 62, "F4", 63, "F5", 64, "F6", 65, "F7", 66, "F8", 67, "F9",
                68, "F10", 69, "NUMLOCK", 70, "SCROLL", 71, "NUMPAD7", 72, "NUMPAD8", 73, "NUMPAD9", 74, "SUBTRACT", 75, "NUMPAD4",
                76, "NUMPAD5", 77, "NUMPAD6", 78, "ADD", 79, "NUMPAD1", 80, "NUMPAD2", 81, "NUMPAD3", 82, "NUMPAD0", 83, "DECIMAL",
                87, "F11", 88, "F12", 156, "NUMPADENTER", 157, "RCONTROL", 181, "DIVIDE", 183, "SYSRQ", 184, "RMENU", 197, "PAUSE",
                199, "HOME", 200, "UP", 201, "PRIOR", 203, "LEFT", 205, "RIGHT", 207, "END", 208, "DOWN", 209, "NEXT", 210, "INSERT",
                211, "DELETE", 219, "LMETA", 220, "RMETA"};
        for (int i = 0; i < names.length; i += 2) KEY_NAMES.put((Integer) names[i], (String) names[i + 1]);
    }

    public static String getKeyName(int key) {
        String fixed = KEY_NAMES.get(key);
        if (fixed != null) return fixed;
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
