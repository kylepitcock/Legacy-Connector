package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;

/**
 * 1.12.2's slider of the world customisation pages (not FML's config slider): a track with an 8 pixel handle, its value between min and
 * max shown as "name: value" (or through a {@link FormatHelper}) and reported to a {@link GuiPageButtonList.GuiResponder}.
 */
public class GuiSlider extends GuiButton {
    private float sliderPosition = 1.0F;
    public boolean isMouseDown;
    private final String name;
    private final float min;
    private final float max;
    private final GuiPageButtonList.GuiResponder responder;
    private FormatHelper formatHelper;

    public GuiSlider(GuiPageButtonList.GuiResponder guiResponder, int idIn, int x, int y, String nameIn, float minIn, float maxIn, float defaultValue, FormatHelper formatter) {
        super(idIn, x, y, 150, 20, "");
        this.name = nameIn;
        this.min = minIn;
        this.max = maxIn;
        this.sliderPosition = (defaultValue - minIn) / (maxIn - minIn);
        this.formatHelper = formatter;
        this.responder = guiResponder;
        this.displayString = getDisplayString();
    }

    public float getSliderValue() {
        return min + (max - min) * sliderPosition;
    }

    public void setSliderValue(float value, boolean notifyResponder) {
        sliderPosition = (value - min) / (max - min);
        displayString = getDisplayString();
        if (notifyResponder) notifyResponder();
    }

    public float getSliderPosition() {
        return sliderPosition;
    }

    private void notifyResponder() {
        if (responder != null) responder.setEntryValue(id, getSliderValue());
    }

    private String getDisplayString() {
        return formatHelper == null ? I18n.format(name) + ": " + getSliderValue() : formatHelper.getText(id, I18n.format(name), getSliderValue());
    }

    /** The track is the disabled button face. */
    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    /** Follows the mouse while held, then draws the handle (called from drawButton, as in 1.12). */
    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        if (isMouseDown) {
            sliderPosition = clamp((float) (mouseX - (x + 4)) / (float) (width - 8));
            displayString = getDisplayString();
            notifyResponder();
        }
        mcc$drawBackground(1, x + (int) (sliderPosition * (float) (width - 8)), y, 8, height);
    }

    private static float clamp(float f) {
        return f < 0.0F ? 0.0F : f > 1.0F ? 1.0F : f;
    }

    public void setSliderPosition(float position) {
        sliderPosition = position;
        displayString = getDisplayString();
        notifyResponder();
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) return false;
        sliderPosition = clamp((float) (mouseX - (x + 4)) / (float) (width - 8));
        displayString = getDisplayString();
        notifyResponder();
        isMouseDown = true;
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        isMouseDown = false;
    }

    public interface FormatHelper {
        String getText(int id, String name, float value);
    }
}
