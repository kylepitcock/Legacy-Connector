package org.kylepitcock.legacyconnector.api;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection helpers for calling legacy mod APIs from an addon, which cannot name the legacy classes at compile time (their
 * parameter types are the shim's legacy-Minecraft classes). Methods are found by <em>name and argument values</em>: the first public
 * (or accessible) method of that name whose parameters accept the arguments, preferring exact matches; {@code null} matches any
 * reference parameter. Resolved handles are cached per (class, name, argument classes).
 *
 * <pre>
 * Class&lt;?&gt; api = LegacyApi.classOf("thaumcraft", "thaumcraft.api.ThaumcraftApi").orElseThrow();
 * Object aspects = LegacyAccess.newInstance(aspectListClass);
 * LegacyAccess.invoke(aspects, "add", LegacyAccess.getStatic(aspectClass, "METAL"), 5);
 * LegacyAccess.invokeStatic(api, "registerObjectTag", LegacyItems.toLegacy(stack, LegacyVersion.V1_7_10), aspects);
 * </pre>
 *
 * Primitive parameters accept the matching wrapper ({@code 5} for an {@code int}); results of primitive type come back boxed.
 * Varargs methods must be given the array explicitly. Failures throw {@link LegacyAccessException}; an exception thrown by the
 * called method is its {@linkplain Throwable#getCause() cause}.
 */
public final class LegacyAccess {
    private static final Map<String, MethodHandle> HANDLES = new ConcurrentHashMap<>();
    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();

    /** Thrown when a legacy member cannot be found or the call fails. */
    public static final class LegacyAccessException extends RuntimeException {
        public LegacyAccessException(String message, Throwable cause) {
            super(message, cause);
        }

        public LegacyAccessException(String message) {
            super(message);
        }
    }

    private LegacyAccess() {}

    /** Calls the static method {@code name} of {@code cls}. */
    public static Object invokeStatic(Class<?> cls, String name, Object... args) {
        return call(cls, name, null, args, true);
    }

    /** Calls the instance method {@code name} of {@code target} (public or inherited, any visibility is made accessible). */
    public static Object invoke(Object target, String name, Object... args) {
        if (target == null) throw new LegacyAccessException("Cannot call " + name + " on null");
        return call(target.getClass(), name, target, args, false);
    }

    /** True if {@code cls} (or a superclass) has a method of this name taking this many arguments. */
    public static boolean hasMethod(Class<?> cls, String name, int argCount) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) if (m.getName().equals(name) && m.getParameterCount() == argCount) return true;
        }
        for (Method m : cls.getMethods()) if (m.getName().equals(name) && m.getParameterCount() == argCount) return true;
        return false;
    }

    /** Constructs {@code cls} with the constructor accepting {@code args}. */
    public static Object newInstance(Class<?> cls, Object... args) {
        String key = cls.getName() + "#<init>" + signature(args);
        try {
            MethodHandle handle = HANDLES.get(key);
            if (handle == null) {
                Constructor<?> best = pick(cls.getDeclaredConstructors(), args);
                if (best == null) throw new LegacyAccessException("No constructor of " + cls.getName() + " accepts " + signature(args));
                best.setAccessible(true);
                handle = MethodHandles.lookup().unreflectConstructor(best).asFixedArity();
                HANDLES.put(key, handle);
            }
            return handle.invokeWithArguments(args);
        } catch (LegacyAccessException e) {
            throw e;
        } catch (Throwable t) {
            throw new LegacyAccessException("Constructing " + cls.getName() + " failed: " + t, t);
        }
    }

    /** The value of the static field {@code name} of {@code cls}. */
    public static Object getStatic(Class<?> cls, String name) {
        return get(field(cls, name), null);
    }

    /** The value of the instance field {@code name} of {@code target}. */
    public static Object getField(Object target, String name) {
        return get(field(target.getClass(), name), target);
    }

    public static void setField(Object target, String name, Object value) {
        try {
            field(target.getClass(), name).set(target, value);
        } catch (IllegalAccessException e) {
            throw new LegacyAccessException("Cannot set " + name + ": " + e, e);
        }
    }

    public static void setStatic(Class<?> cls, String name, Object value) {
        try {
            field(cls, name).set(null, value);
        } catch (IllegalAccessException e) {
            throw new LegacyAccessException("Cannot set " + name + ": " + e, e);
        }
    }

    /** True if {@code obj} is an instance of the class {@code className} (class or interface) as seen by obj's own class loader. */
    public static boolean isInstance(Object obj, String className) {
        if (obj == null) return false;
        for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().equals(className) || implementsInterface(c, className)) return true;
        }
        return false;
    }

    private static boolean implementsInterface(Class<?> c, String name) {
        for (Class<?> i : c.getInterfaces()) if (i.getName().equals(name) || implementsInterface(i, name)) return true;
        return false;
    }

    // ------------------------------------------------------------------ internals

    private static Object get(Field f, Object target) {
        try {
            return f.get(target);
        } catch (IllegalAccessException e) {
            throw new LegacyAccessException("Cannot read " + f + ": " + e, e);
        }
    }

    private static Field field(Class<?> cls, String name) {
        String key = cls.getName() + "#" + name;
        Field cached = FIELDS.get(key);
        if (cached != null) return cached;
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                FIELDS.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
                // look in the superclass
            }
        }
        throw new LegacyAccessException("No field " + name + " in " + cls.getName());
    }

    private static Object call(Class<?> cls, String name, Object target, Object[] args, boolean isStatic) {
        String key = cls.getName() + "#" + name + signature(args) + (isStatic ? "S" : "I");
        try {
            MethodHandle handle = HANDLES.get(key);
            if (handle == null) {
                Method best = null;
                for (Class<?> c = cls; c != null && best == null; c = c.getSuperclass()) best = pick(candidates(c, name, isStatic), args);
                if (best == null) best = pick(candidates(cls, name, isStatic, true), args);
                if (best == null) throw new LegacyAccessException("No " + (isStatic ? "static " : "") + "method " + name + signature(args) + " in " + cls.getName());
                best.setAccessible(true);
                handle = MethodHandles.lookup().unreflect(best).asFixedArity();
                HANDLES.put(key, handle);
            }
            if (isStatic) return handle.invokeWithArguments(args);
            Object[] all = new Object[args.length + 1];
            all[0] = target;
            System.arraycopy(args, 0, all, 1, args.length);
            return handle.invokeWithArguments(all);
        } catch (LegacyAccessException e) {
            throw e;
        } catch (Throwable t) {
            throw new LegacyAccessException("Calling " + cls.getName() + "." + name + " failed: " + t, t);
        }
    }

    private static Method[] candidates(Class<?> c, String name, boolean isStatic) {
        return candidates(c, name, isStatic, false);
    }

    /** Methods of this class (declared; or all public ones incl. interface defaults when {@code publicOnes}) with the name. */
    private static Method[] candidates(Class<?> c, String name, boolean isStatic, boolean publicOnes) {
        return Arrays.stream(publicOnes ? c.getMethods() : c.getDeclaredMethods())
                .filter(m -> m.getName().equals(name) && Modifier.isStatic(m.getModifiers()) == isStatic && !m.isBridge())
                .toArray(Method[]::new);
    }

    private static <E extends Executable> E pick(E[] options, Object[] args) {
        E best = null;
        int bestScore = -1;
        for (E e : options) {
            int score = score(e.getParameterTypes(), args);
            if (score > bestScore) {
                best = e;
                bestScore = score;
            }
        }
        return best;
    }

    /** -1 if the parameters cannot take the arguments, else the number of exact matches. */
    private static int score(Class<?>[] params, Object[] args) {
        if (params.length != args.length) return -1;
        int exact = 0;
        for (int i = 0; i < params.length; i++) {
            Object a = args[i];
            Class<?> p = params[i];
            if (a == null) {
                if (p.isPrimitive()) return -1;
                continue;
            }
            Class<?> boxed = p.isPrimitive() ? box(p) : p;
            if (!boxed.isInstance(a)) {
                if (!(p.isPrimitive() && widens(boxed, a))) return -1;
            } else if (boxed == a.getClass()) {
                exact++;
            }
        }
        return exact;
    }

    /** Integer arguments for long/float/double parameters and the like. */
    private static boolean widens(Class<?> target, Object a) {
        if (!(a instanceof Number) && !(a instanceof Character)) return false;
        List<Class<?>> order = List.of(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class);
        int from = order.indexOf(a.getClass());
        int to = order.indexOf(target);
        return from >= 0 && to > from;
    }

    private static Class<?> box(Class<?> p) {
        if (p == int.class) return Integer.class;
        if (p == boolean.class) return Boolean.class;
        if (p == long.class) return Long.class;
        if (p == float.class) return Float.class;
        if (p == double.class) return Double.class;
        if (p == byte.class) return Byte.class;
        if (p == short.class) return Short.class;
        if (p == char.class) return Character.class;
        return p;
    }

    private static String signature(Object[] args) {
        StringBuilder sb = new StringBuilder("(");
        for (Object a : args) sb.append(a == null ? "null" : a.getClass().getName()).append(',');
        return sb.append(')').toString();
    }
}
