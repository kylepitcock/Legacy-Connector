package mccompat.v1_7_10.org.lwjgl.input;

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

    public static int getDX() {
        return 0;
    }

    public static int getDY() {
        return 0;
    }

    public static int getDWheel() {
        return 0;
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

    public static int getEventButton() {
        return -1;
    }

    public static boolean getEventButtonState() {
        return false;
    }

    public static int getEventDWheel() {
        return 0;
    }

    public static void setCursorPosition(int x, int y) {
    }

    public static int getButtonCount() {
        return 8;
    }
}
