package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;

/** A slider for an option (FOV, sensitivity, render distance...): dragging it sets the game's option and shows the value. */
public class GuiOptionSlider extends GuiButton {
    private float sliderValue;
    public boolean dragging;
    private final GameSettings.Options options;
    private final float minValue;
    private final float maxValue;

    public GuiOptionSlider(int buttonId, int x, int y, GameSettings.Options option) {
        this(buttonId, x, y, option, 0.0F, 1.0F);
    }

    public GuiOptionSlider(int buttonId, int x, int y, GameSettings.Options option, float minValue, float maxValue) {
        super(buttonId, x, y, 150, 20, "");
        this.options = option;
        this.minValue = minValue;
        this.maxValue = maxValue;
        GameSettings settings = settings();
        this.sliderValue = option.normalizeValue(settings.getOptionFloatValue(option));
        this.displayString = settings.getKeyBinding(option);
    }

    private static GameSettings settings() {
        return Minecraft.mcc$get$gameSettings(Minecraft.getMinecraft());
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    private void mcc$apply(int mouseX) {
        sliderValue = Math.max(0.0F, Math.min(1.0F, (float) (mouseX - (x + 4)) / (float) (width - 8)));
        float value = options.denormalizeValue(sliderValue);
        settings().setOptionFloatValue(options, value);
        sliderValue = options.normalizeValue(value);
        displayString = settings().getKeyBinding(options);
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        if (dragging) mcc$apply(mouseX);
        mcc$drawBackground(1, x + (int) (sliderValue * (float) (width - 8)), y, 8, height);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) return false;
        mcc$apply(mouseX);
        dragging = true;
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }
}
