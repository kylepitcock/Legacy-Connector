package legacyconnector.v1_12_2.net.minecraft.block.properties;

public abstract class PropertyHelper<T extends Comparable<T>> implements IProperty<T> {
    private final Class<T> valueClass;
    private final String name;

    protected PropertyHelper(String name, Class<T> valueClass) {
        this.valueClass = valueClass;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getValueClass() {
        return valueClass;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + name;
    }
}
