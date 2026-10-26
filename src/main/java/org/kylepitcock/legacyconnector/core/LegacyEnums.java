package org.kylepitcock.legacyconnector.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge's runtime enum extension ({@code EnumHelper}) for every legacy version. Works for any enum (shim, synthesized or
 * the mod's own): the constant is built through a method handle (reflection refuses to construct enums) and appended
 * to {@code $VALUES}.
 */
public final class LegacyEnums {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/EnumHelper");

    private LegacyEnums() {}

    /** Parameter types inferred from the values (boxed types matched against primitive constructor parameters). */
    public static <T extends Enum<?>> T add(Class<T> enumType, String enumName, Object... paramValues) {
        for (Constructor<?> c : enumType.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length != paramValues.length + 2) continue;
            Class<?>[] user = java.util.Arrays.copyOfRange(p, 2, p.length);
            if (matches(user, paramValues)) return add(enumType, enumName, user, paramValues);
        }
        LOG.warn("No constructor of {} matches {} for {}", enumType.getName(), java.util.Arrays.toString(paramValues), enumName);
        return null;
    }

    /** Forge's form with a table of {enum class, parameter types...} rows. */
    public static <T extends Enum<?>> T add(Class<?>[][] map, Class<T> enumType, String enumName, Object... paramValues) {
        for (Class<?>[] lookup : map) {
            if (lookup[0] == enumType) return add(enumType, enumName, java.util.Arrays.copyOfRange(lookup, 1, lookup.length), paramValues);
        }
        return null;
    }

    /** Adds (or returns the existing) constant {@code enumName}, built by the constructor taking {@code paramTypes}. */
    @SuppressWarnings("unchecked")
    public static synchronized <T extends Enum<?>> T add(Class<T> enumType, String enumName, Class<?>[] paramTypes, Object[] paramValues) {
        try {
            Field valuesField = valuesField(enumType);
            T[] previous = (T[]) valuesField.get(null);
            for (T existing : previous) if (existing.name().equals(enumName)) return existing;

            Class<?>[] all = new Class<?>[paramTypes.length + 2];
            all[0] = String.class;
            all[1] = int.class;
            System.arraycopy(paramTypes, 0, all, 2, paramTypes.length);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(enumType, MethodHandles.lookup());
            // asFixedArity: an enum constructor ending in a varargs parameter must receive its array as given, not be re-collected
            MethodHandle ctor = lookup.findConstructor(enumType, MethodType.methodType(void.class, all)).asFixedArity();
            Object[] args = new Object[all.length];
            args[0] = enumName;
            args[1] = previous.length;
            System.arraycopy(paramValues, 0, args, 2, paramValues.length);
            T value = (T) ctor.invokeWithArguments(args);

            T[] values = (T[]) Array.newInstance(enumType, previous.length + 1);
            System.arraycopy(previous, 0, values, 0, previous.length);
            values[previous.length] = value;
            ReflectionCompat.set(valuesField, null, values);
            resetEnumCaches(enumType);
            return value;
        } catch (Throwable t) {
            LOG.error("Could not add {} to {}", enumName, enumType.getName(), t);
            return null;
        }
    }

    /** Writes a field even when it is final (static finals included), as Forge's {@code setFailsafeFieldValue} did. */
    public static void setFailsafeFieldValue(Field field, Object target, Object value) throws IllegalAccessException {
        try {
            field.setAccessible(true);
        } catch (RuntimeException ignored) {
            // the Unsafe fallback of ReflectionCompat.set does not need access checks
        }
        ReflectionCompat.set(field, target, value);
    }

    private static Field valuesField(Class<?> enumType) throws NoSuchFieldException {
        for (Field f : enumType.getDeclaredFields()) {
            if (f.isSynthetic() && Modifier.isStatic(f.getModifiers()) && f.getType().isArray() && f.getType().getComponentType() == enumType) {
                f.setAccessible(true);
                return f;
            }
        }
        Field f = enumType.getDeclaredField("$VALUES");
        f.setAccessible(true);
        return f;
    }

    /** Class caches values() results; drop them so valueOf/EnumMap see the new constant. */
    private static void resetEnumCaches(Class<?> enumType) {
        for (String name : new String[] {"enumConstants", "enumConstantDirectory"}) {
            try {
                Field f = Class.class.getDeclaredField(name);
                ReflectionCompat.set(f, enumType, null);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // best effort; values() itself reads $VALUES
            }
        }
    }

    private static boolean matches(Class<?>[] types, Object[] values) {
        for (int i = 0; i < types.length; i++) {
            Object v = values[i];
            Class<?> t = types[i].isPrimitive() ? box(types[i]) : types[i];
            if (v != null && !t.isInstance(v)) return false;
            if (v == null && types[i].isPrimitive()) return false;
        }
        return true;
    }

    private static Class<?> box(Class<?> p) {
        return MethodType.methodType(p).wrap().returnType();
    }
}
