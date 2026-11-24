package legacyconnector.vb1_7_3.net.minecraft.client.gui.widget;

import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.option.Option;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;

/**
 * Beta's {@code GuiSmallButton} and {@code GuiSlider}: the half-width button of the options screens, tied to one {@link Option}. A slider
 * option keeps its value 0..1 and sets it where the button is clicked (the knob is drawn on the bar).
 */
public class OptionButtonWidget extends ButtonWidget {
    private final Option option;
    public float sliderValue = 1.0F;

    public OptionButtonWidget(int id, int x, int y, String text) { this(id, x, y, null, text); }

    public OptionButtonWidget(int id, int x, int y, Option option, String text) {
        super(id, x, y, 150, 20, text);
        this.option = option;
    }

    public OptionButtonWidget(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
        this.option = null;
    }

    public OptionButtonWidget(int id, int x, int y, Option option, String text, float value) {
        this(id, x, y, option, text);
        sliderValue = value;
    }

    /** Beta's {@code returnEnumOptions}. */
    public Option getOption() { return option; }

    private boolean slider() { return option != null && option.isSlider(); }

    @Override protected int getYImage(boolean hovered) { return slider() ? 0 : super.getYImage(hovered); }

    @Override protected void renderBackground(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible || !slider()) return;
        GuiDrawB173.bind(GuiDrawB173.textureId("/gui/gui.png"));
        GuiDrawB173.color(1.0F, 1.0F, 1.0F, 1.0F);
        int knob = x + (int) (sliderValue * (width - 8));
        drawTexture(knob, y, 0, 66, 4, 20);
        drawTexture(knob + 4, y, 196, 66, 4, 20);
    }

    /** A click on a slider sets its value from the click position (0 at the left edge, 1 at the right). */
    @Override public boolean isMouseOver(Minecraft minecraft, int mouseX, int mouseY) {
        boolean over = super.isMouseOver(minecraft, mouseX, mouseY);
        if (over && slider()) {
            sliderValue = Math.max(0.0F, Math.min(1.0F, (float) (mouseX - (x + 4)) / (float) (width - 8)));
            if (minecraft != null && minecraft.options != null) {
                minecraft.options.setFloat(option, sliderValue);
                text = minecraft.options.getString(option);
            }
        }
        return over;
    }
}
