package org.kylepitcock.legacyconnector.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Writes static fields, final ones included (Forge filled mods' static final fields reflectively). */
public final class StaticFields {
    private StaticFields() {}

    public static void set(Field f, Object value) throws ReflectiveOperationException {
        f.setAccessible(true);
        if (!Modifier.isFinal(f.getModifiers())) {
            f.set(null, value);
            return;
        }
        Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        unsafe.ensureClassInitialized(f.getDeclaringClass());
        unsafe.putObjectVolatile(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }
}
