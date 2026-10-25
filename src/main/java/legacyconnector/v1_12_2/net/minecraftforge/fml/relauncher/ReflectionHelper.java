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
        public UnableToFindFieldException(String[] names, Exception cause) {
            super("Could not find any of " + java.util.Arrays.toString(names), cause);
        }
    }

    public static class UnableToAccessFieldException extends RuntimeException {
        public UnableToAccessFieldException(String[] names, Exception cause) {
            super("Could not access " + java.util.Arrays.toString(names), cause);
        }
    }

    private static boolean ours(Class<?> c) {
        String n = c.getName();
        return n.startsWith("legacyconnector.") || n.startsWith("legacyconnector_synth.");
    }

    public static Field findField(Class<?> clazz, String... fieldNames) {
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
