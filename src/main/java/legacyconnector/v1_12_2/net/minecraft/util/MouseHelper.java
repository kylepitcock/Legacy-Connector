package legacyconnector.v1_12_2.net.minecraft.util;

/**
 * The mouse in game: grabbing and releasing the cursor (the modern mouse handler does it) and the movement since the last call
 * ({@link #deltaX}, {@link #deltaY}; y grows upwards as with LWJGL 2).
 */
public class MouseHelper {
    public int deltaX;
    public int deltaY;
    private double mcc$lastX = Double.NaN;
    private double mcc$lastY = Double.NaN;

    public void grabMouseCursor() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null) mc.mouseHandler.grabMouse();
        deltaX = 0;
        deltaY = 0;
        mcc$lastX = Double.NaN;
    }

    public void ungrabMouseCursor() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null) mc.mouseHandler.releaseMouse();
    }

    public void mouseXYChange() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) return;
        double x = mc.mouseHandler.xpos();
        double y = mc.mouseHandler.ypos();
        if (Double.isNaN(mcc$lastX)) {
            deltaX = 0;
            deltaY = 0;
        } else {
            deltaX = (int) (x - mcc$lastX);
            deltaY = (int) (mcc$lastY - y);
        }
        mcc$lastX = x;
        mcc$lastY = y;
    }
}
