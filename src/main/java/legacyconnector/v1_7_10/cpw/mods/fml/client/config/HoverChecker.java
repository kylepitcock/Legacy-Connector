package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;

/** Tells when the mouse has stayed over a region (or a button) for a threshold, for delayed tooltips. */
public class HoverChecker {
    private int top;
    private int bottom;
    private int left;
    private int right;
    private int threshold;
    private GuiButton button;
    private long hoverStart;

    public HoverChecker(int top, int bottom, int left, int right, int threshold) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        this.threshold = threshold;
        this.hoverStart = -1;
    }

    public HoverChecker(GuiButton button, int threshold) {
        this.button = button;
        this.threshold = threshold;
        this.hoverStart = -1;
    }

    public void updateBounds(int top, int bottom, int left, int right) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }

    public boolean checkHover(int mouseX, int mouseY) {
        return checkHover(mouseX, mouseY, true);
    }

    public boolean checkHover(int mouseX, int mouseY, boolean canHover) {
        if (button != null) {
            top = button.yPosition;
            bottom = button.yPosition + button.height;
            left = button.xPosition;
            right = button.xPosition + button.width;
            canHover = canHover && button.visible;
        }
        boolean inside = mouseY >= top && mouseY <= bottom && mouseX >= left && mouseX <= right;
        if (canHover && hoverStart == -1 && inside) hoverStart = System.currentTimeMillis();
        else if (!canHover || !inside) resetHoverTimer();
        return canHover && hoverStart != -1 && System.currentTimeMillis() - hoverStart >= threshold;
    }

    public void resetHoverTimer() {
        hoverStart = -1;
    }
}
