package org.kylepitcock.legacyconnector.compat.jdk;

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
        // the stand-in for LaunchWrapper's runTransformers handed out by getDeclaredMethod: nothing is left to transform
        if (method.equals(RUN_TRANSFORMERS) && !(method.getDeclaringClass().isInstance(target))) return args[2];
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() != null && INVOCATION_FAILURES.incrementAndGet() <= 40) {
                // mods' event dispatchers swallow these: name the root cause and where it happened
                Throwable root = e.getCause();
                while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                StringBuilder at = new StringBuilder();
                int shown = 0;
                for (StackTraceElement f : root.getStackTrace()) {
                    String c = f.getClassName();
                    if (shown > 0 && (c.startsWith("java.") || c.startsWith("jdk.") || c.startsWith("com.google."))) continue;
                    at.append(" at ").append(f.getClassName()).append('.').append(f.getMethodName()).append(':').append(f.getLineNumber());
                    if (++shown == 4) break;
                }
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Reflection").info("{}.{} threw {}{}", method.getDeclaringClass().getName(), method.getName(),
                        e.getCause().toString(), root == e.getCause() ? "" : " (root: " + root + at + ")");
            }
            throw e;
        }
    }

    private static final java.lang.reflect.Method RUN_TRANSFORMERS = runTransformers();
    private static final java.util.concurrent.atomic.AtomicInteger NULLED_STATICS = new java.util.concurrent.atomic.AtomicInteger();

    private static java.lang.reflect.Method runTransformers() {
        try {
            return legacyconnector.v1_7_10.net.minecraft.launchwrapper.LaunchClassLoader.class.getDeclaredMethod("runTransformers", String.class, String.class, byte[].class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Legacy mods found LaunchWrapper's class loader under their classes and called its private {@code runTransformers} on
     * class bytes they generated (AE2's multipart layers); the modern loader has no such method, so ours stands in.
     */
    public static java.lang.reflect.Method getDeclaredMethod(Class<?> owner, String name, Class<?>... types) throws NoSuchMethodException {
        try {
            return owner.getDeclaredMethod(name, types);
        } catch (NoSuchMethodException e) {
            if (RUN_TRANSFORMERS != null && name.equals("runTransformers") && ClassLoader.class.isAssignableFrom(owner)) return RUN_TRANSFORMERS;
            String mcp = SrgNames.method(owner, name);
            if (mcp != null) {
                try {
                    return owner.getDeclaredMethod(mcp, types);
                } catch (NoSuchMethodException ignored) {
                    // report the name the mod asked for
                }
            }
            throw e;
        }
    }

    /*
     * Production mods reflect on game classes with SRG names (FMLForgePlugin.RUNTIME_DEOBF is true, as in a real 1.7.10
     * client: DragonAPI's ReikaObfuscationHelper picks "func_..." names by it). Literal SRG ids were renamed by the loader;
     * names built at run time are mapped here when the plain lookup fails, then looked up again the same way.
     */

    public static Field getDeclaredField(Class<?> owner, String name) throws NoSuchFieldException {
        if (owner == Field.class && name.equals("modifiers")) return MODIFIERS_PLACEHOLDER;
        try {
            return owner.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            String mcp = SrgNames.field(owner, name);
            if (mcp != null) {
                try {
                    return owner.getDeclaredField(mcp);
                } catch (NoSuchFieldException ignored) {
                    // report the name the mod asked for
                }
            }
            throw e;
        }
    }

    public static Field getField(Class<?> owner, String name) throws NoSuchFieldException {
        try {
            return owner.getField(name);
        } catch (NoSuchFieldException e) {
            String mcp = SrgNames.field(owner, name);
            if (mcp != null) {
                try {
                    return owner.getField(mcp);
                } catch (NoSuchFieldException ignored) {
                    // report the name the mod asked for
                }
            }
            throw e;
        }
    }

    public static java.lang.reflect.Method getMethod(Class<?> owner, String name, Class<?>... types) throws NoSuchMethodException {
        try {
            return owner.getMethod(name, types);
        } catch (NoSuchMethodException e) {
            String mcp = SrgNames.method(owner, name);
            if (mcp != null) {
                try {
                    return owner.getMethod(mcp, types);
                } catch (NoSuchMethodException ignored) {
                    // report the name the mod asked for
                }
            }
            throw e;
        }
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
        if (value == null && target == null && field.getDeclaringClass().getName().startsWith("legacyconnector.") && NULLED_STATICS.incrementAndGet() <= 5) {
            // a mod clearing a game static (Biomes O' Plenty's vanilla biomes): name who did it
            StackTraceElement[] st = new Throwable().getStackTrace();
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Reflection").info("{}.{} set to null by {}", field.getDeclaringClass().getName(), field.getName(),
                    st.length > 1 ? st[1] : "?");
        }
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
        // a null here is the mod's own bug (a failed lookup); the forced write below would crash the JVM on it
        if (object == null) throw new NullPointerException("Cannot invoke \"java.lang.reflect.AccessibleObject.setAccessible(boolean)\" because the reflected member is null");
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
