package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;

/** FML's slider: a track (the disabled button face) with an 8 pixel handle, showing prefix + value + suffix. */
public class GuiSlider extends GuiButtonExt {
    /** Position of the handle, 0 to 1. */
    public double sliderValue = 1.0F;
    public String dispString = "";
    public boolean dragging = false;
    public boolean showDecimal = true;
    public double minValue = 0.0D;
    public double maxValue = 5.0D;
    public int precision = 1;
    public ISlider parent = null;
    public String suffix = "";
    public boolean drawString = true;

    public GuiSlider(int id, int xPos, int yPos, int width, int height, String prefix, String suf, double minVal, double maxVal, double currentVal,
            boolean showDec, boolean drawStr) {
        this(id, xPos, yPos, width, height, prefix, suf, minVal, maxVal, currentVal, showDec, drawStr, null);
    }

    public GuiSlider(int id, int xPos, int yPos, int width, int height, String prefix, String suf, double minVal, double maxVal, double currentVal,
            boolean showDec, boolean drawStr, ISlider par) {
        super(id, xPos, yPos, width, height, prefix);
        minValue = minVal;
        maxValue = maxVal;
        sliderValue = (currentVal - minValue) / (maxValue - minValue);
        dispString = prefix;
        parent = par;
        suffix = suf;
        showDecimal = showDec;
        String val;
        if (showDecimal) {
            val = Double.toString(sliderValue * (maxValue - minValue) + minValue);
            precision = Math.min(val.substring(val.indexOf(".") + 1).length(), 4);
        } else {
            val = Integer.toString((int) Math.round(sliderValue * (maxValue - minValue) + minValue));
            precision = 0;
        }
        displayString = dispString + val + suffix;
        drawString = drawStr;
        if (!drawString) displayString = "";
    }

    public GuiSlider(int id, int xPos, int yPos, String displayStr, double minVal, double maxVal, double currentVal, ISlider par) {
        this(id, xPos, yPos, 150, 20, displayStr, "", minVal, maxVal, currentVal, true, true, par);
    }

    /** The track never highlights. */
    @Override
    public int getHoverState(boolean hovered) {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        if (dragging) {
            sliderValue = (mouseX - (xPosition + 4)) / (float) (width - 8);
            updateSlider();
        }
        mcc$drawBackground(1, xPosition + (int) (sliderValue * (float) (width - 8)), yPosition, 8, height);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            sliderValue = (float) (mouseX - (xPosition + 4)) / (float) (width - 8);
            updateSlider();
            dragging = true;
            return true;
        }
        return false;
    }

    public void updateSlider() {
        if (sliderValue < 0.0F) sliderValue = 0.0F;
        if (sliderValue > 1.0F) sliderValue = 1.0F;
        String val;
        if (showDecimal) {
            val = Double.toString(sliderValue * (maxValue - minValue) + minValue);
            if (val.substring(val.indexOf(".") + 1).length() > precision) {
                val = val.substring(0, val.indexOf(".") + precision + 1);
                if (val.endsWith(".")) val = val.substring(0, val.indexOf(".") + precision);
            } else {
                while (val.substring(val.indexOf(".") + 1).length() < precision) val = val + "0";
            }
        } else {
            val = Integer.toString((int) Math.round(sliderValue * (maxValue - minValue) + minValue));
        }
        if (drawString) displayString = dispString + val + suffix;
        if (parent != null) parent.onChangeSliderValue(this);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }

    public int getValueInt() {
        return (int) Math.round(sliderValue * (maxValue - minValue) + minValue);
    }

    public double getValue() {
        return sliderValue * (maxValue - minValue) + minValue;
    }

    public void setValue(double d) {
        this.sliderValue = (d - minValue) / (maxValue - minValue);
    }

    public interface ISlider {
        void onChangeSliderValue(GuiSlider slider);
    }
}
