package legacyconnector.v1_6_4.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings;

/**
 * 1.6.4's slider of the options screens ({@code GuiSlider}): a button that drags one float option of the game settings, with the label
 * the screen passes in and the initial value it read. Public {@code sliderValue} and {@code dragging} as in 1.6.4.
 */
public class GuiSlider extends GuiButton {
    /** The slider position, 0 to 1. */
    public float sliderValue;
    /** Whether the mouse is dragging the handle. */
    public boolean dragging;
    private final GameSettings.Options idFloat;
    private final String label;

    public GuiSlider(int id, int x, int y, GameSettings.Options option, String label, float value) {
        super(id, x, y, 150, 20, label);
        this.idFloat = option;
        this.label = label;
        this.sliderValue = value;
        this.displayString = label + value;
    }

    @Override
    public int getHoverState(boolean hovered) {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        if (dragging) {
            sliderValue = Math.max(0.0F, Math.min(1.0F, (float) (mouseX - (xPosition + 4)) / (float) (width - 8)));
            apply(mc);
        }
        mcc$drawBackground(hovered ? 2 : 1, xPosition + (int) (sliderValue * (float) (width - 8)), yPosition, 8, 20);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) return false;
        sliderValue = Math.max(0.0F, Math.min(1.0F, (float) (mouseX - (xPosition + 4)) / (float) (width - 8)));
        apply(mc);
        dragging = true;
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }

    private void apply(Minecraft mc) {
        GameSettings settings = Minecraft.mcc$get$gameSettings(mc);
        settings.setOptionFloatValue(idFloat, idFloat.denormalizeValue(sliderValue));
        sliderValue = idFloat.normalizeValue(settings.getOptionFloatValue(idFloat));
        displayString = settings.getKeyBinding(idFloat);
    }
}
