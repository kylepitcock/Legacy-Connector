package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;

/**
 * The GUI-scaled size of the window. Legacy code constructs one every frame to place HUD and screens; the numbers come from
 * the modern window, whose GUI scale is what the player chose.
 */
public class ScaledResolution {
    private final int scaledWidth;
    private final int scaledHeight;
    private final double scaledWidthD;
    private final double scaledHeightD;
    private final int scaleFactor;

    public ScaledResolution(Minecraft mc, int displayWidth, int displayHeight) {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        this.scaleFactor = Math.max(1, (int) window.getGuiScale());
        this.scaledWidth = window.getGuiScaledWidth();
        this.scaledHeight = window.getGuiScaledHeight();
        this.scaledWidthD = (double) window.getWidth() / scaleFactor;
        this.scaledHeightD = (double) window.getHeight() / scaleFactor;
    }

    public int getScaledWidth() {
        return scaledWidth;
    }

    public int getScaledHeight() {
        return scaledHeight;
    }

    public double getScaledWidth_double() {
        return scaledWidthD;
    }

    public double getScaledHeight_double() {
        return scaledHeightD;
    }

    public int getScaleFactor() {
        return scaleFactor;
    }
}
