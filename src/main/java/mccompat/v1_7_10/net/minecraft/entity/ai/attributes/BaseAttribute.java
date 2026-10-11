package mccompat.v1_7_10.net.minecraft.entity.ai.attributes;

public abstract class BaseAttribute implements IAttribute {
    private final String unlocalizedName;
    private final double defaultValue;
    private boolean shouldWatch;

    protected BaseAttribute(String name, double defaultValue) {
        this.unlocalizedName = name;
        this.defaultValue = defaultValue;
        if (name == null) throw new IllegalArgumentException("Name cannot be null!");
    }

    @Override
    public String getAttributeUnlocalizedName() {
        return unlocalizedName;
    }

    @Override
    public double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean getShouldWatch() {
        return shouldWatch;
    }

    public BaseAttribute setShouldWatch(boolean watch) {
        this.shouldWatch = watch;
        return this;
    }

    @Override
    public int hashCode() {
        return unlocalizedName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IAttribute a && unlocalizedName.equals(a.getAttributeUnlocalizedName());
    }
}
