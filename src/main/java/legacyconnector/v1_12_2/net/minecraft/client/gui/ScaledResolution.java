package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;

/** The GUI size: the window size divided by the GUI scale, as the modern window computes it. */
public class ScaledResolution {
    private final double scaledWidthD;
    private final double scaledHeightD;
    private final int scaledWidth;
    private final int scaledHeight;
    private final int scaleFactor;

    public ScaledResolution(Minecraft mc) {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        this.scaleFactor = Math.max(1, (int) Math.round(window.getGuiScale()));
        this.scaledWidthD = (double) window.getWidth() / (double) scaleFactor;
        this.scaledHeightD = (double) window.getHeight() / (double) scaleFactor;
        this.scaledWidth = (int) Math.ceil(scaledWidthD);
        this.scaledHeight = (int) Math.ceil(scaledHeightD);
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
