package legacyconnector.v1_7_10.cpw.mods.fml.relauncher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat;

/**
 * FML's reflection helper. Against shim classes, legacy field names refer to shim fields; values of "phantom"
 * fields (views over modern state) are read through their accessors so reflection agrees with field access.
 */
public class ReflectionHelper {
    public static class UnableToFindFieldException extends RuntimeException {
        public UnableToFindFieldException(String[] names, Exception e) {
            super(String.join(",", names), e);
        }
    }

    public static class UnableToAccessFieldException extends RuntimeException {
        public UnableToAccessFieldException(String[] names, Exception e) {
            super(String.join(",", names), e);
        }
    }

    public static class UnableToFindMethodException extends RuntimeException {
        public UnableToFindMethodException(String[] names, Exception e) {
            super(String.join(",", names), e);
        }
    }

    public static Field findField(Class<?> cls, String... names) {
        Exception last = null;
        for (String n : names) {
            try {
                Field f = cls.getDeclaredField(n);
                ReflectionCompat.setAccessible(f, true);
                return f;
            } catch (Exception e) {
                last = e;
            }
        }
        throw new UnableToFindFieldException(names, last);
    }

    /** Declared fields as the legacy class had them: the shim's own bookkeeping fields do not shift the indices. */
    private static Field[] legacyFields(Class<?> cls) {
        return java.util.Arrays.stream(cls.getDeclaredFields()).filter(f -> !f.getName().startsWith("mcc$")).toArray(Field[]::new);
    }

    private static final String SHIM = "legacyconnector.v1_7_10.";
    private static volatile java.util.Map<String, String[]> vanillaOrder;

    /**
     * Field number {@code index} of {@code cls}: for shim classes of vanilla ones the field vanilla 1.7.10 declared at that
     * position (the shim declares its fields in another order: Aether's renderer set EntityRenderer #7, the item renderer,
     * and hit locationLightMap), else the class's own declaration order.
     */
    private static Field indexed(Class<?> cls, int index) {
        if (cls.getName().startsWith(SHIM + "net.minecraft.")) {
            String[] order = vanillaOrder().get(cls.getName().substring(SHIM.length()).replace('.', '/'));
            if (order != null && index >= 0 && index < order.length) {
                try {
                    return findField(cls, order[index]);
                } catch (UnableToFindFieldException ignored) {
                    // a field the shim does not declare: fall back to the shim's order
                }
            }
        }
        Field f = legacyFields(cls)[index];
        ReflectionCompat.setAccessible(f, true);
        return f;
    }

    private static java.util.Map<String, String[]> vanillaOrder() {
        var order = vanillaOrder;
        if (order != null) return order;
        order = new java.util.HashMap<>();
        try (var in = ReflectionHelper.class.getResourceAsStream("/legacyconnector/v1_7_10/field-order.txt")) {
            if (in != null) {
                for (String line : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                    String[] p = line.trim().split(" ");
                    if (p.length > 1) order.put(p[0], java.util.Arrays.copyOfRange(p, 1, p.length));
                }
            }
        } catch (java.io.IOException ignored) {
            // no table: indices follow the shim's declaration order
        }
        return vanillaOrder = order;
    }

    @SuppressWarnings("unchecked")
    public static <T, E> T getPrivateValue(Class<? super E> cls, E instance, int fieldIndex) {
        try {
            Field f = indexed(cls, fieldIndex);
            return (T) read(f, instance);
        } catch (Exception e) {
            throw new UnableToAccessFieldException(new String[] {"#" + fieldIndex}, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, E> T getPrivateValue(Class<? super E> cls, E instance, String... names) {
        try {
            return (T) read(findField(cls, names), instance);
        } catch (UnableToFindFieldException e) {
            throw e;
        } catch (Exception e) {
            throw new UnableToAccessFieldException(names, e);
        }
    }

    public static <T, E> void setPrivateValue(Class<? super T> cls, T instance, E value, int fieldIndex) {
        try {
            Field f = indexed(cls, fieldIndex);
            write(f, instance, value);
        } catch (Exception e) {
            throw new UnableToAccessFieldException(new String[] {"#" + fieldIndex}, e);
        }
    }

    public static <T, E> void setPrivateValue(Class<? super T> cls, T instance, E value, String... names) {
        try {
            write(findField(cls, names), instance, value);
        } catch (UnableToFindFieldException e) {
            throw e;
        } catch (Exception e) {
            throw new UnableToAccessFieldException(names, e);
        }
    }

    public static Method findMethod(Class<?> cls, Object instance, String[] names, Class<?>... params) {
        Exception last = null;
        for (String n : names) {
            try {
                Method m = cls.getDeclaredMethod(n, params);
                m.setAccessible(true);
                return m;
            } catch (Exception e) {
                last = e;
            }
        }
        throw new UnableToFindMethodException(names, last);
    }

    public static Class<?> getClass(ClassLoader loader, String... names) {
        for (String n : names) {
            try {
                return Class.forName(n, false, loader);
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("Unable to find any of " + String.join(",", names));
    }

    private static Object read(Field f, Object instance) throws Exception {
        Method getter = accessor(f, "mcc$get$");
        if (getter != null) return java.lang.reflect.Modifier.isStatic(f.getModifiers()) ? getter.invoke(null) : getter.invoke(null, instance);
        return ReflectionCompat.get(f, instance);
    }

    private static void write(Field f, Object instance, Object value) throws Exception {
        Method setter = accessor(f, "mcc$set$");
        if (setter != null) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) setter.invoke(null, value);
            else setter.invoke(null, instance, value);
            return;
        }
        ReflectionCompat.set(f, instance, value);
    }

    private static Method accessor(Field f, String prefix) {
        for (Method m : f.getDeclaringClass().getDeclaredMethods()) {
            if (m.getName().equals(prefix + f.getName())) return m;
        }
        return null;
    }
}
