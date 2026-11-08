package legacyconnector.v1_20_1.net.minecraftforge.client.gui.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Forge 1.20.1's slider with a real value range, step and precision (the vanilla slider only knows 0 to 1). */
public class ForgeSlider extends AbstractSliderButton {
    protected final Component prefix;
    protected final Component suffix;
    protected final double minValue;
    protected final double maxValue;
    protected final double stepSize;
    protected final int precision;
    protected final boolean drawString;

    public ForgeSlider(int x, int y, int width, int height, Component prefix, Component suffix, double minValue, double maxValue, double currentValue,
            double stepSize, int precision, boolean drawString) {
        super(x, y, width, height, CommonComponents.EMPTY, 0.0);
        this.prefix = prefix;
        this.suffix = suffix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepSize = Math.abs(stepSize);
        this.value = snapToNearest((currentValue - minValue) / (maxValue - minValue));
        this.precision = precision;
        this.drawString = drawString;
        updateMessage();
    }

    public ForgeSlider(int x, int y, int width, int height, Component prefix, Component suffix, double minValue, double maxValue, double currentValue,
            boolean drawString) {
        this(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, 1, 0, drawString);
    }

    public double getValue() {
        return value * (maxValue - minValue) + minValue;
    }

    public int getValueInt() {
        return (int) Math.round(getValue());
    }

    public long getValueLong() {
        return Math.round(getValue());
    }

    public void setValue(double newValue) {
        this.value = snapToNearest((newValue - minValue) / (maxValue - minValue));
        updateMessage();
    }

    private double snapToNearest(double newValue) {
        if (stepSize <= 0.0) return Mth.clamp(newValue, 0.0, 1.0);
        newValue = Mth.lerp(Mth.clamp(newValue, 0.0, 1.0), minValue, maxValue);
        newValue = stepSize * Math.round(newValue / stepSize);
        if (minValue > maxValue) newValue = Mth.clamp(newValue, maxValue, minValue);
        else newValue = Mth.clamp(newValue, minValue, maxValue);
        return Mth.map(newValue, minValue, maxValue, 0.0, 1.0);
    }

    protected void applyValue() {
    }

    protected void updateMessage() {
        if (drawString) setMessage(Component.literal("").append(prefix).append(getValueString()).append(suffix));
        else setMessage(Component.empty());
    }

    public String getValueString() {
        return String.format("%." + precision + "f", getValue());
    }
}
