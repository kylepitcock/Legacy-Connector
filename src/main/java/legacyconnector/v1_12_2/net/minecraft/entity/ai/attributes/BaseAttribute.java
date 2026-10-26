package legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes;

public abstract class BaseAttribute implements IAttribute {
    private final IAttribute parent;
    private final String translationKey;
    private final double defaultValue;
    private boolean shouldWatch;

    protected BaseAttribute(IAttribute parent, String name, double defaultValue) {
        this.parent = parent;
        this.translationKey = name;
        this.defaultValue = defaultValue;
        if (name == null) throw new IllegalArgumentException("Name cannot be null!");
    }

    @Override
    public String getName() {
        return translationKey;
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
    public IAttribute getParent() {
        return parent;
    }

    @Override
    public int hashCode() {
        return translationKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IAttribute a && translationKey.equals(a.getName());
    }
}
