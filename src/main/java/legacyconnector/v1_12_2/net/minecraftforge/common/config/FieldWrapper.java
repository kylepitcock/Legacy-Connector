package legacyconnector.v1_12_2.net.minecraftforge.common.config;

/** Forge's adapter between {@code @Config} fields and config properties; only the check which fields it can store is needed. */
public abstract class FieldWrapper {
    private static final java.util.Set<Class<?>> SIMPLE = java.util.Set.of(boolean.class, Boolean.class, int.class, Integer.class, float.class, Float.class,
            double.class, Double.class, byte.class, Byte.class, char.class, Character.class, short.class, Short.class, long.class, Long.class, String.class,
            boolean[].class, Boolean[].class, int[].class, Integer[].class, float[].class, Float[].class, double[].class, Double[].class, byte[].class,
            Byte[].class, char[].class, Character[].class, short[].class, Short[].class, long[].class, Long[].class, String[].class);

    /** Whether a field is a value the config stores itself (simple values, enums and string-keyed maps; anything else is a sub-category). */
    public static boolean hasWrapperFor(java.lang.reflect.Field field) {
        Class<?> t = field.getType();
        return SIMPLE.contains(t) || Enum.class.isAssignableFrom(t) || java.util.Map.class.isAssignableFrom(t);
    }
}
