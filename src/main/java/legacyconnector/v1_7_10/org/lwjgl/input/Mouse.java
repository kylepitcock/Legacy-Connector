package legacyconnector.v1_7_10.org.lwjgl.input;

/** LWJGL 2 mouse polling on top of the modern game's window. */
public class Mouse {
    public static boolean isButtonDown(int button) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) return false;
            if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), button) == org.lwjgl.glfw.GLFW.GLFW_PRESS) return true;
            // a vanilla binding on this button held down without the physical button (the remote-control test client)
            if (mc.options == null) return false;
            for (var key : new net.minecraft.client.KeyMapping[] {mc.options.keyAttack, mc.options.keyUse, mc.options.keyPickItem}) {
                var k = key.getKey();
                if (k.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE && k.getValue() == button && key.isDown()) return true;
            }
            return false;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /**
     * What {@link #setCursorPosition} moved the pointer by, relative to the game's own tracked position. GLFW delivers the cursor callback
     * later (and not at all while the pointer is grabbed, where the tracked position is a free-running virtual one: values like -4549), so
     * LWJGL 2's "set, then read back what was set" is kept by remembering the difference and applying it to what the game reports.
     */
    private static volatile double offsetX;
    private static volatile double offsetY;
    private static volatile double baseX = Double.NaN;
    private static volatile double baseY = Double.NaN;

    /** The offset while it still applies: until the game's own position moved on (the pointer is free), always while the pointer is grabbed. */
    private static double offsetX() {
        var h = net.minecraft.client.Minecraft.getInstance().mouseHandler;
        if (!h.isMouseGrabbed() && h.xpos() != baseX) offsetX = 0;
        return offsetX;
    }

    private static double offsetY() {
        var h = net.minecraft.client.Minecraft.getInstance().mouseHandler;
        if (!h.isMouseGrabbed() && h.ypos() != baseY) offsetY = 0;
        return offsetY;
    }

    public static int getX() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return (int) (mc.mouseHandler.xpos() + offsetX());
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    /** LWJGL 2's origin is the bottom left. */
    public static int getY() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return (int) (mc.getWindow().getHeight() - (int) mc.mouseHandler.ypos() - 1 + offsetY());
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    private static double lastX = Double.NaN;
    private static double lastY = Double.NaN;
    private static volatile int wheel;

    /** Scroll steps (one notch is 120, as in LWJGL 2) the game has seen since the last {@link #getDWheel()}. */
    public static void mcc$scrolled(double vertical) {
        wheel += (int) Math.round(vertical * 120.0D);
    }

    /** Movement since the last call, in window pixels (up is positive in LWJGL 2's y axis). */
    public static int getDX() {
        try {
            double x = net.minecraft.client.Minecraft.getInstance().mouseHandler.xpos();
            int d = Double.isNaN(lastX) ? 0 : (int) (x - lastX);
            lastX = x;
            return d;
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static int getDY() {
        try {
            double y = net.minecraft.client.Minecraft.getInstance().mouseHandler.ypos();
            int d = Double.isNaN(lastY) ? 0 : (int) (lastY - y);
            lastY = y;
            return d;
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static int getEventX() {
        return getX();
    }

    public static int getEventY() {
        return getY();
    }

    public static int getEventDX() {
        return getDX();
    }

    public static int getEventDY() {
        return getDY();
    }

    public static int getDWheel() {
        int w = wheel;
        wheel = 0;
        return w;
    }

    public static boolean isGrabbed() {
        try {
            return net.minecraft.client.Minecraft.getInstance().mouseHandler.isMouseGrabbed();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void setGrabbed(boolean grab) {
    }

    public static boolean isInsideWindow() {
        return true;
    }

    public static boolean isCreated() {
        return true;
    }

    public static boolean next() {
        return false;
    }

    /** The button event being handled (FML's MouseInputEvent and Forge's MouseEvent handlers read it); -1 for none. */
    private static volatile int eventButton = -1;
    private static volatile boolean eventButtonState;

    public static void mcc$setEvent(int button, boolean state) {
        eventButton = button;
        eventButtonState = state;
    }

    public static int getEventButton() {
        return eventButton;
    }

    public static boolean getEventButtonState() {
        return eventButtonState;
    }

    /** While a legacy screen handles one mouse event ({@code GuiScreen.handleMouseInput}), its own wheel movement (0 for clicks). */
    private static volatile int eventWheel;
    private static volatile boolean inScreenEvent;

    public static void mcc$setEventWheel(int dwheel, boolean active) {
        eventWheel = dwheel;
        inScreenEvent = active;
    }

    public static int getEventDWheel() {
        return inScreenEvent ? eventWheel : wheel;
    }

    /** Moves the pointer to (x, y) in window pixels, y from the bottom (LWJGL 2's origin). */
    public static void setCursorPosition(int x, int y) {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        if (window == null) return;
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(window.getWindow(), x, window.getScreenHeight() - y);
        try {
            var handler = net.minecraft.client.Minecraft.getInstance().mouseHandler;
            baseX = handler.xpos();
            baseY = handler.ypos();
            offsetX = x - handler.xpos();
            offsetY = y - (window.getHeight() - (int) handler.ypos() - 1);
        } catch (RuntimeException | LinkageError e) {
            offsetX = 0;
            offsetY = 0;
        }
    }

    private static volatile Cursor nativeCursor;

    static Cursor mcc$currentCursor() {
        return nativeCursor;
    }

    /**
     * Shows a native cursor over the window (null restores the arrow) and returns the one shown before. The GLFW cursor is created and set on the
     * render thread (now when called there).
     */
    public static Cursor setNativeCursor(Cursor cursor) throws legacyconnector.v1_7_10.org.lwjgl.LWJGLException {
        Cursor previous = nativeCursor;
        nativeCursor = cursor;
        var mc = net.minecraft.client.Minecraft.getInstance();
        Runnable apply = () -> {
            Cursor wanted = nativeCursor;
            long handle = wanted == null ? 0L : wanted.mcc$handle();
            org.lwjgl.glfw.GLFW.glfwSetCursor(mc.getWindow().getWindow(), handle);
        };
        if (mc.isSameThread()) apply.run();
        else mc.execute(apply);
        return previous;
    }


    public static int getButtonCount() {
        return 8;
    }

    /** The mouse always has a wheel on GLFW. */
    public static boolean hasWheel() {
        return true;
    }

    /** The game owns the mouse (GLFW); mods that recreate it to change its mode get a no-op. */
    public static void create() {
    }

    public static void destroy() {
    }

    /** LWJGL 2's button names: "BUTTON0", "BUTTON1", ... */
    public static String getButtonName(int button) {
        return button >= 0 && button < getButtonCount() ? "BUTTON" + button : null;
    }

    /** LWJGL 2's {@code getButtonIndex(name)}: the index of "BUTTON0", "BUTTON1", ... or -1. */
    public static int getButtonIndex(String name) {
        for (int i = 0; i < getButtonCount(); i++) if (("BUTTON" + i).equalsIgnoreCase(name)) return i;
        return -1;
    }

    /** When the current event happened (System.nanoTime() clock; events are delivered as they occur). */
    public static long getEventNanoseconds() {
        return System.nanoTime();
    }
}
