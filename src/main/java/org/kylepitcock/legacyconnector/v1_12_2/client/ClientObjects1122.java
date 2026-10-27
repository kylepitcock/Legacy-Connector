package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Makes the client singletons {@code Minecraft} hands out (render manager, entity renderer, render global, framebuffer...) without
 * depending on how their shim classes are built: a static {@code mcc$instance()} (or {@code mcc$main()}) is preferred, then the
 * legacy constructor taking the given arguments, then any constructor (fed nulls and zeros). Each class is made once by the caller.
 */
public final class ClientObjects1122 {
    private ClientObjects1122() {}

    public static <T> T make(Class<T> type, Object... args) {
        for (String name : new String[] {"mcc$instance", "mcc$main"}) {
            try {
                Method m = type.getDeclaredMethod(name);
                if (Modifier.isStatic(m.getModifiers()) && type.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    Object made = m.invoke(null);
                    if (made != null) return type.cast(made);
                }
            } catch (NoSuchMethodException e) {
                // try the next way
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Client").debug("{}.{}() failed: {}", type.getName(), name, e.toString());
            }
        }
        Constructor<?> best = null;
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length == args.length && accepts(params, args)) {
                best = c;
                break;
            }
            if (best == null || params.length < best.getParameterCount()) best = c;
        }
        if (best == null) return null;
        try {
            best.setAccessible(true);
            Class<?>[] params = best.getParameterTypes();
            Object[] values = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                values[i] = i < args.length && (args[i] == null || box(params[i]).isInstance(args[i])) ? args[i] : zero(params[i]);
            }
            return type.cast(best.newInstance(values));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Client").warn("Could not make the legacy {}: {}", type.getName(), e.toString());
            return null;
        }
    }

    private static boolean accepts(Class<?>[] params, Object[] args) {
        for (int i = 0; i < params.length; i++) {
            if (args[i] == null ? params[i].isPrimitive() : !box(params[i]).isInstance(args[i])) return false;
        }
        return true;
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == long.class) return Long.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        return Character.class;
    }

    private static Object zero(Class<?> c) {
        if (!c.isPrimitive()) return null;
        if (c == boolean.class) return false;
        if (c == char.class) return '\0';
        if (c == float.class) return 0.0F;
        if (c == double.class) return 0.0D;
        if (c == long.class) return 0L;
        if (c == short.class) return (short) 0;
        if (c == byte.class) return (byte) 0;
        return 0;
    }
}
