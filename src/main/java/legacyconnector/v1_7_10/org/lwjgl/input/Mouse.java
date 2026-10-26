package legacyconnector.v1_7_10.org.lwjgl.input;

/** LWJGL 2 mouse polling on top of the modern game's window. */
public class Mouse {
    public static boolean isButtonDown(int button) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) return false;
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), button) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static int getX() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return (int) mc.mouseHandler.xpos();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    /** LWJGL 2's origin is the bottom left. */
    public static int getY() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return mc.getWindow().getHeight() - (int) mc.mouseHandler.ypos() - 1;
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

    public static int getEventDWheel() {
        return wheel;
    }

    public static void setCursorPosition(int x, int y) {
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

    /** When the current event happened (System.nanoTime() clock; events are delivered as they occur). */
    public static long getEventNanoseconds() {
        return System.nanoTime();
    }
}
