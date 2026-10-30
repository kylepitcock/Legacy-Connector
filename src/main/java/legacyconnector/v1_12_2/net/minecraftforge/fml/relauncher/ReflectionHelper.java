package legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Forge's reflection helper: members are looked up by a list of names (deobfuscated first, then SRG). Translated mods use MCP names; a member the
 * shim or the synthesizer does not have gives a harmless stand-in instead of an exception, so the mod can go on without that private access.
 */
public class ReflectionHelper {
    /** Holder of the stand-in field and method. */
    private static class Dummy {
        public static Object value;

        public static Object noop(Object... args) {
            return null;
        }
    }

    public static class UnableToFindMethodException extends RuntimeException {
        public UnableToFindMethodException(Throwable cause) {
            super(cause);
        }

        public UnableToFindMethodException(String[] names, Exception cause) {
            super("Could not find any of " + java.util.Arrays.toString(names), cause);
        }
    }

    public static class UnableToFindFieldException extends RuntimeException {
        public UnableToFindFieldException(Exception cause) {
            super(cause);
        }

        public UnableToFindFieldException(String[] names, Exception cause) {
            super("Could not find any of " + java.util.Arrays.toString(names), cause);
        }
    }

    public static class UnableToAccessFieldException extends RuntimeException {
        public UnableToAccessFieldException(Exception cause) {
            super(cause);
        }

        public UnableToAccessFieldException(String[] names, Exception cause) {
            super("Could not access " + java.util.Arrays.toString(names), cause);
        }
    }

    private static boolean ours(Class<?> c) {
        String n = c.getName();
        return n.startsWith("legacyconnector.") || n.startsWith("legacyconnector_synth.");
    }

    /** MCP names of SRG field names (field_12345_a), from the same table the loader uses to link mods; loaded on first use. */
    private static volatile java.util.Map<String, String> srgFields;

    private static String[] withMcpNames(String[] names) {
        java.util.List<String> out = null;
        for (String n : names) {
            if (!n.startsWith("field_")) continue;
            java.util.Map<String, String> table = srgFields;
            if (table == null) {
                table = new java.util.HashMap<>();
                try (var in = ReflectionHelper.class.getClassLoader().getResourceAsStream("legacyconnector/legacy/1.12.2/fields.txt")) {
                    if (in != null) {
                        for (String line : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
                            String[] p = line.split(" ");
                            if (p.length >= 3 && p[0].startsWith("field_")) table.put(p[0], p[2]);
                        }
                    }
                } catch (java.io.IOException | RuntimeException e) {
                    // no table: SRG names stay unresolved
                }
                srgFields = table;
            }
            String mcp = table.get(n);
            if (mcp != null) {
                if (out == null) out = new java.util.ArrayList<>(java.util.Arrays.asList(names));
                out.add(mcp);
            }
        }
        return out == null ? names : out.toArray(new String[0]);
    }

    public static Field findField(Class<?> clazz, String... fieldNames) {
        fieldNames = withMcpNames(fieldNames);
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (String name : fieldNames) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException | RuntimeException e) {
                    // try the next name or class
                }
            }
        }
        if (ours(clazz)) {
            try {
                return Dummy.class.getField("value");
            } catch (NoSuchFieldException e) {
                throw new UnableToFindFieldException(fieldNames, e);
            }
        }
        throw new UnableToFindFieldException(fieldNames, null);
    }

    /** The 1.12.2 form: deobfuscated and SRG name. */
    public static Field findField(Class<?> clazz, String fieldName, String fieldObfName) {
        return findField(clazz, new String[] {fieldName, fieldObfName});
    }

    public static Method findMethod(Class<?> clazz, String methodName, String methodObfName, Class<?>... parameterTypes) {
        return findMethod((Class<Object>) clazz, null, new String[] {methodName, methodObfName}, parameterTypes);
    }

    @SuppressWarnings("unchecked")
    public static <E> Method findMethod(Class<? super E> clazz, E instance, String[] methodNames, Class<?>... methodTypes) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (String name : methodNames) {
                try {
                    Method m = c.getDeclaredMethod(name, methodTypes);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException | RuntimeException e) {
                    // try the next name or class
                }
            }
        }
        if (ours(clazz)) {
            try {
                return Dummy.class.getMethod("noop", Object[].class);
            } catch (NoSuchMethodException e) {
                throw new UnableToFindMethodException(methodNames, e);
            }
        }
        throw new UnableToFindMethodException(methodNames, null);
    }

    @SuppressWarnings("unchecked")
    public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, int fieldIndex) {
        try {
            Field f = classToAccess.getDeclaredFields()[fieldIndex];
            f.setAccessible(true);
            return (T) f.get(instance);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new UnableToAccessFieldException(new String[0], e instanceof Exception ex ? ex : null);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, String... fieldNames) {
        try {
            Field f = findField(classToAccess, fieldNames);
            if (f.getDeclaringClass() == Dummy.class) return null;
            return (T) f.get(instance);
        } catch (IllegalAccessException e) {
            throw new UnableToAccessFieldException(fieldNames, e);
        }
    }

    public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, int fieldIndex) {
        try {
            Field f = classToAccess.getDeclaredFields()[fieldIndex];
            f.setAccessible(true);
            f.set(instance, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new UnableToAccessFieldException(new String[0], e instanceof Exception ex ? ex : null);
        }
    }

    public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, String... fieldNames) {
        try {
            Field f = findField(classToAccess, fieldNames);
            if (f.getDeclaringClass() == Dummy.class) return;
            if (java.lang.reflect.Modifier.isFinal(f.getModifiers()) && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                org.kylepitcock.legacyconnector.core.StaticFields.set(f, value);
            } else {
                f.set(instance, value);
            }
        } catch (ReflectiveOperationException e) {
            throw new UnableToAccessFieldException(fieldNames, e);
        }
    }

    /** A field's value by its deobfuscated or its obfuscated (SRG) name. */
    public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, String fieldName, String fieldObfName) {
        return getPrivateValue(classToAccess, instance, new String[] {fieldName, fieldObfName});
    }

    /** Sets a field found by its deobfuscated or its obfuscated (SRG) name. */
    public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, String fieldName, String fieldObfName) {
        setPrivateValue(classToAccess, instance, value, new String[] {fieldName, fieldObfName});
    }

    /** The (accessible) constructor of {@code klass} with these parameter types. */
    public static <T> java.lang.reflect.Constructor<T> findConstructor(Class<T> klass, Class<?>... parameterTypes) {
        try {
            java.lang.reflect.Constructor<T> c = klass.getDeclaredConstructor(parameterTypes);
            c.setAccessible(true);
            return c;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new UnknownConstructorException("Could not find constructor " + java.util.Arrays.toString(parameterTypes) + " in " + klass, e);
        }
    }

    public static class UnknownConstructorException extends RuntimeException {
        public UnknownConstructorException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static Class<?> getClass(ClassLoader loader, String... classNames) {
        for (String name : classNames) {
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException e) {
                // try the next name
            }
        }
        throw new RuntimeException("Could not find any of " + java.util.Arrays.toString(classNames));
    }
}
