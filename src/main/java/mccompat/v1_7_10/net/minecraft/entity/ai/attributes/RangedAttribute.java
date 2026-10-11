package mccompat.v1_7_10.net.minecraft.entity.ai.attributes;

public class RangedAttribute extends BaseAttribute {
    private final double minimumValue;
    private final double maximumValue;
    private String description;

    public RangedAttribute(String name, double defaultValue, double min, double max) {
        super(name, defaultValue);
        this.minimumValue = min;
        this.maximumValue = max;
        if (min > max) throw new IllegalArgumentException("Minimum value cannot be bigger than maximum value!");
        if (defaultValue < min) throw new IllegalArgumentException("Default value cannot be lower than minimum value!");
        if (defaultValue > max) throw new IllegalArgumentException("Default value cannot be bigger than maximum value!");
    }

    public RangedAttribute setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public double clampValue(double value) {
        return Math.max(minimumValue, Math.min(maximumValue, value));
    }
}
