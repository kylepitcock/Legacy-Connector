package org.kylepitcock.legacyconnector.core.degraded;

import java.util.concurrent.ConcurrentHashMap;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;

/** The owning mod of a legacy class, by the class loader the class came from (cached per loader). */
public final class DegradedOwners {
    private static final ConcurrentHashMap<ClassLoader, String> BY_LOADER = new ConcurrentHashMap<>();

    private DegradedOwners() {}

    /** The legacy id of the mod whose class loader loaded {@code cls}; else the package root of the class name; never null. */
    public static String of(Class<?> cls) {
        if (cls == null) return Degraded.OTHER;
        try {
            ClassLoader loader = cls.getClassLoader();
            if (loader != null && loader != DegradedOwners.class.getClassLoader()) {
                String cached = BY_LOADER.get(loader);
                if (cached == null) {
                    for (LegacyModRecord m : LegacyMods.all()) {
                        if (m.mainClass != null && m.mainClass.getClassLoader() == loader) {
                            cached = m.legacyId;
                            break;
                        }
                    }
                    // a miss is not cached: mods are still being registered early on
                    if (cached != null) BY_LOADER.put(loader, cached);
                }
                if (cached != null) return cached;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // fall through to the class name
        }
        return ofClassName(cls.getName());
    }

    /** The mod of an instance (its class). */
    public static String ofInstance(Object o) {
        return o == null ? Degraded.OTHER : of(o.getClass());
    }

    /** A class name reduced to its first three package segments, for classes no mod claims. */
    public static String ofClassName(String className) {
        if (className == null) return Degraded.OTHER;
        int dots = 0;
        for (int i = 0; i < className.length(); i++) {
            if (className.charAt(i) == '.' && ++dots == 3) return className.substring(0, i);
        }
        return className;
    }
}
