package org.kylepitcock.mccompat.compat.jdk;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import sun.misc.Unsafe;

/**
 * Restores the classic "make a final field writable" reflection hack.
 *
 * <p>Java 8 mods did {@code Field m = Field.class.getDeclaredField("modifiers"); m.setInt(f, f.getModifiers() & ~FINAL);
 * f.set(null, value)}. Since Java 12 {@code modifiers} is hidden, and since Java 18 static finals cannot be
 * written reflectively at all. The transformer redirects those calls here; writes to final fields that the JVM
 * refuses fall back to {@link Unsafe}.</p>
 */
@SuppressWarnings({"deprecation", "removal"})
public final class ReflectionCompat {
    private static final Unsafe UNSAFE;
    /** Stand-in returned for {@code Field.class.getDeclaredField("modifiers")}. */
    @SuppressWarnings("unused")
    private static int modifiers;
    private static final Field MODIFIERS_PLACEHOLDER;
    private static final Set<Field> UNFINALIZED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
            MODIFIERS_PLACEHOLDER = ReflectionCompat.class.getDeclaredField("modifiers");
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ReflectionCompat() {}

    private static final java.util.concurrent.atomic.AtomicInteger INVOCATION_FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    /** Method.invoke that records what an InvocationTargetException wraps (callers often log only the wrapper). */
    public static Object invoke(java.lang.reflect.Method method, Object target, Object... args)
            throws IllegalAccessException, java.lang.reflect.InvocationTargetException {
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() != null && INVOCATION_FAILURES.incrementAndGet() <= 40) {
                org.slf4j.LoggerFactory.getLogger("Retrofit/Reflection").info("{}.{} threw {}", method.getDeclaringClass().getName(), method.getName(), e.getCause().toString());
            }
            throw e;
        }
    }

    public static Field getDeclaredField(Class<?> owner, String name) throws NoSuchFieldException {
        if (owner == Field.class && name.equals("modifiers")) return MODIFIERS_PLACEHOLDER;
        return owner.getDeclaredField(name);
    }

    public static void setInt(Field field, Object target, int value) throws IllegalAccessException {
        if (field.equals(MODIFIERS_PLACEHOLDER)) {
            if (target instanceof Field f && (value & Modifier.FINAL) == 0) UNFINALIZED.add(f);
            return;
        }
        try {
            field.setInt(target, value);
        } catch (IllegalAccessException e) {
            if (!unsafePut(field, target, value)) throw e;
        }
    }

    public static void set(Field field, Object target, Object value) throws IllegalAccessException {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            if (!unsafePut(field, target, value)) throw e;
        }
    }

    public static void setBoolean(Field field, Object target, boolean value) throws IllegalAccessException {
        try {
            field.setBoolean(target, value);
        } catch (IllegalAccessException e) {
            if (!unsafePut(field, target, value)) throw e;
        }
    }

    public static void setLong(Field field, Object target, long value) throws IllegalAccessException {
        try {
            field.setLong(target, value);
        } catch (IllegalAccessException e) {
            if (!unsafePut(field, target, value)) throw e;
        }
    }

    public static void setFloat(Field field, Object target, float value) throws IllegalAccessException {
        try {
            field.setFloat(target, value);
        } catch (IllegalAccessException e) {
            if (!unsafePut(field, target, value)) throw e;
        }
    }

    public static void setDouble(Field field, Object target, double value) throws IllegalAccessException {
        try {
            field.setDouble(target, value);
        } catch (IllegalAccessException e) {
            if (!unsafePut(field, target, value)) throw e;
        }
    }

    /** Writes a final field the JVM refused to write. Only used for fields that are actually final. */
    private static boolean unsafePut(Field field, Object target, Object value) {
        // Reached when the JVM refused the write: a final field, or a private field of a JDK class.
        if (!isStaticOk(field, target)) return false;
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        Object base = isStatic ? UNSAFE.staticFieldBase(field) : target;
        long offset = isStatic ? UNSAFE.staticFieldOffset(field) : UNSAFE.objectFieldOffset(field);
        Class<?> t = field.getType();
        if (t == int.class) UNSAFE.putIntVolatile(base, offset, ((Number) value).intValue());
        else if (t == long.class) UNSAFE.putLongVolatile(base, offset, ((Number) value).longValue());
        else if (t == boolean.class) UNSAFE.putBooleanVolatile(base, offset, (Boolean) value);
        else if (t == float.class) UNSAFE.putFloatVolatile(base, offset, ((Number) value).floatValue());
        else if (t == double.class) UNSAFE.putDoubleVolatile(base, offset, ((Number) value).doubleValue());
        else if (t == short.class) UNSAFE.putShortVolatile(base, offset, ((Number) value).shortValue());
        else if (t == byte.class) UNSAFE.putByteVolatile(base, offset, ((Number) value).byteValue());
        else if (t == char.class) UNSAFE.putCharVolatile(base, offset, (Character) value);
        else UNSAFE.putObjectVolatile(base, offset, value);
        return true;
    }

    private static boolean isStaticOk(Field field, Object target) {
        return Modifier.isStatic(field.getModifiers()) || target != null;
    }

    /**
     * {@code setAccessible(true)} that tolerates the module system's refusal (private fields of JDK classes, which
     * legacy mods reflected into freely on Java 8): reads and writes of such fields then go through Unsafe.
     */
    /** Layout twin of AccessibleObject's first field, to find where {@code override} lives (the field itself is hidden). */
    @SuppressWarnings("unused")
    private static final class OverrideProbe {
        boolean override;
    }

    public static void setAccessible(java.lang.reflect.AccessibleObject object, boolean flag) {
        try {
            object.setAccessible(flag);
        } catch (RuntimeException e) {
            // InaccessibleObjectException (a JDK member in a module that is not opened to the mod): the legacy mod ran
            // on a JVM with no such protection, so force the override flag. Fields also have the get/set fallbacks.
            if (flag) {
                try {
                    UNSAFE.putBoolean(object, UNSAFE.objectFieldOffset(OverrideProbe.class.getDeclaredField("override")), true);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // stays inaccessible
                }
            }
        }
    }

    public static void setAccessible(java.lang.reflect.Method method, boolean flag) {
        setAccessible((java.lang.reflect.AccessibleObject) method, flag);
    }

    public static void setAccessible(java.lang.reflect.Constructor<?> constructor, boolean flag) {
        setAccessible((java.lang.reflect.AccessibleObject) constructor, flag);
    }

    public static void setAccessible(Field field, boolean flag) {
        setAccessible((java.lang.reflect.AccessibleObject) field, flag);
    }

    public static Object get(Field field, Object target) throws IllegalAccessException {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            if (!isStaticOk(field, target)) throw e;
            return unsafeGet(field, target);
        }
    }

    public static int getInt(Field field, Object target) throws IllegalAccessException {
        return ((Number) get(field, target)).intValue();
    }

    public static long getLong(Field field, Object target) throws IllegalAccessException {
        return ((Number) get(field, target)).longValue();
    }

    public static boolean getBoolean(Field field, Object target) throws IllegalAccessException {
        return (Boolean) get(field, target);
    }

    public static float getFloat(Field field, Object target) throws IllegalAccessException {
        return ((Number) get(field, target)).floatValue();
    }

    public static double getDouble(Field field, Object target) throws IllegalAccessException {
        return ((Number) get(field, target)).doubleValue();
    }

    private static Object unsafeGet(Field field, Object target) {
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        Object base = isStatic ? UNSAFE.staticFieldBase(field) : target;
        long offset = isStatic ? UNSAFE.staticFieldOffset(field) : UNSAFE.objectFieldOffset(field);
        Class<?> t = field.getType();
        if (t == int.class) return UNSAFE.getIntVolatile(base, offset);
        if (t == long.class) return UNSAFE.getLongVolatile(base, offset);
        if (t == boolean.class) return UNSAFE.getBooleanVolatile(base, offset);
        if (t == float.class) return UNSAFE.getFloatVolatile(base, offset);
        if (t == double.class) return UNSAFE.getDoubleVolatile(base, offset);
        if (t == short.class) return UNSAFE.getShortVolatile(base, offset);
        if (t == byte.class) return UNSAFE.getByteVolatile(base, offset);
        if (t == char.class) return UNSAFE.getCharVolatile(base, offset);
        return UNSAFE.getObjectVolatile(base, offset);
    }
}
