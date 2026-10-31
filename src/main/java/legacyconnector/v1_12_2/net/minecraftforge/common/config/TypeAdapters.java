package legacyconnector.v1_12_2.net.minecraftforge.common.config;

/** The type adapters Forge has for simple values (constants without bodies so the declared methods are found by reflection). */
public enum TypeAdapters implements ITypeAdapter {
    bool(Property.Type.BOOLEAN), int_(Property.Type.INTEGER), dbl(Property.Type.DOUBLE), str(Property.Type.STRING);

    private final Property.Type type;

    TypeAdapters(Property.Type type) {
        this.type = type;
    }

    @Override
    public Property.Type getType() {
        return type;
    }

    static TypeAdapters of(Class<?> t) {
        while (t.isArray()) t = t.getComponentType();
        if (t == boolean.class || t == Boolean.class) return bool;
        if (t == int.class || t == Integer.class || t == long.class || t == Long.class || t == short.class || t == Short.class
                || t == byte.class || t == Byte.class) return int_;
        if (t == float.class || t == Float.class || t == double.class || t == Double.class) return dbl;
        return str;
    }
}
