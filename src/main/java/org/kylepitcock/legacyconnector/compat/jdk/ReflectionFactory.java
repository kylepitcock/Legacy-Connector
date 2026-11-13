package org.kylepitcock.legacyconnector.compat.jdk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Replacement for {@code sun.reflect.ReflectionFactory}. The JDK class keeps only the serialization constructors (delegated to here); the
 * {@code newConstructorAccessor} that legacy mods used to build enum constants ({@code EnumRarity} and friends, which
 * {@code Constructor.newInstance} rejects) is rebuilt on a method handle, which has no enum check.
 */
public final class ReflectionFactory {
    private static final ReflectionFactory INSTANCE = new ReflectionFactory();

    private ReflectionFactory() {}

    public static ReflectionFactory getReflectionFactory() {
        return INSTANCE;
    }

    public ConstructorAccessor newConstructorAccessor(Constructor<?> constructor) {
        constructor.setAccessible(true);
        final MethodHandle handle;
        try {
            handle = MethodHandles.lookup().unreflectConstructor(constructor).asSpreader(Object[].class, constructor.getParameterCount());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
        return args -> {
            try {
                return handle.invoke(args == null ? new Object[0] : args);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new InvocationTargetException(t);
            }
        };
    }

    public Constructor<?> newConstructorForSerialization(Class<?> cl, Constructor<?> constructorToCall) {
        return sun.reflect.ReflectionFactory.getReflectionFactory().newConstructorForSerialization(cl, constructorToCall);
    }

    public Constructor<?> newConstructorForSerialization(Class<?> cl) {
        return sun.reflect.ReflectionFactory.getReflectionFactory().newConstructorForSerialization(cl);
    }
}
