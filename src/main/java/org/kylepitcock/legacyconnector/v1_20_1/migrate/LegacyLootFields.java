package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.lang.reflect.Field;
import java.util.List;

/** Reads the protected fields of loot classes that the 1.20.1 serializers wrote from (they were accessible there). */
final class LegacyLootFields {
    private LegacyLootFields() {}

    @SuppressWarnings("unchecked")
    static <E> List<E> list(Object owner, Class<?> declaring, String name) {
        Object v = get(owner, declaring, name);
        return v instanceof List<?> l ? (List<E>) l : null;
    }

    static int integer(Object owner, Class<?> declaring, String name, int fallback) {
        return get(owner, declaring, name) instanceof Integer i ? i : fallback;
    }

    private static Object get(Object owner, Class<?> declaring, String name) {
        try {
            Field f = declaring.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
