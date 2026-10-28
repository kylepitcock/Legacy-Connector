package legacyconnector.v1_12_2.org.lwjgl.input;

/** LWJGL 2 mouse polling for 1.12.2 mods: the 1.7.10 shim's implementation on the modern window. */
public class Mouse extends legacyconnector.v1_7_10.org.lwjgl.input.Mouse {
    private static Cursor nativeCursor;

    /** The modern window keeps its own cursor: the legacy one is only remembered, so getNativeCursor answers consistently. */
    public static Cursor setNativeCursor(Cursor cursor) {
        Cursor old = nativeCursor;
        nativeCursor = cursor;
        return old;
    }

    public static Cursor getNativeCursor() {
        return nativeCursor;
    }

    public static void setClipMouseCoordinatesToWindow(boolean clip) {
    }

    /** LWJGL 2 named the buttons "BUTTON0" to "BUTTON15" (-1 for other names). */
    public static int getButtonIndex(String buttonName) {
        if (buttonName == null || !buttonName.startsWith("BUTTON")) return -1;
        try {
            int i = Integer.parseInt(buttonName.substring(6));
            return i >= 0 && i < 16 ? i : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** The cursor is the modern window's. */
    public static void updateCursor() {
    }
}
