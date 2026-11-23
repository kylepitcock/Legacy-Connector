package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings;

/** 1.7.10's slider of the options screens: drags one float option of the game settings between its minimum and maximum. */
public class GuiOptionSlider extends GuiButton {
    private float sliderValue;
    public boolean dragging;
    private final GameSettings.Options idFloat;

    public GuiOptionSlider(int id, int x, int y, GameSettings.Options option) {
        this(id, x, y, option, 0.0F, 1.0F);
    }

    public GuiOptionSlider(int id, int x, int y, GameSettings.Options option, float min, float max) {
        super(id, x, y, 150, 20, "");
        this.idFloat = option;
        GameSettings settings = Minecraft.mcc$get$gameSettings(Minecraft.getMinecraft());
        this.sliderValue = option.normalizeValue(settings.getOptionFloatValue(option));
        this.displayString = settings.getKeyBinding(option);
    }

    @Override
    public int getHoverState(boolean hovered) {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        GameSettings settings = Minecraft.mcc$get$gameSettings(mc);
        if (dragging) {
            sliderValue = Math.max(0.0F, Math.min(1.0F, (float) (mouseX - (xPosition + 4)) / (float) (width - 8)));
            float value = idFloat.denormalizeValue(sliderValue);
            settings.setOptionFloatValue(idFloat, value);
            sliderValue = idFloat.normalizeValue(value);
            displayString = settings.getKeyBinding(idFloat);
        }
        mcc$drawBackground(hovered ? 2 : 1, xPosition + (int) (sliderValue * (float) (width - 8)), yPosition, 8, 20);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) return false;
        sliderValue = Math.max(0.0F, Math.min(1.0F, (float) (mouseX - (xPosition + 4)) / (float) (width - 8)));
        GameSettings settings = Minecraft.mcc$get$gameSettings(mc);
        settings.setOptionFloatValue(idFloat, idFloat.denormalizeValue(sliderValue));
        displayString = settings.getKeyBinding(idFloat);
        dragging = true;
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }
}
