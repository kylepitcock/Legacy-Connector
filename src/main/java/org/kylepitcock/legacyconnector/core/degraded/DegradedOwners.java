package org.kylepitcock.legacyconnector.core.degraded;

import java.util.concurrent.ConcurrentHashMap;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;

/** The owning mod of a legacy class, by the module (jar) or class loader the class came from (cached). */
public final class DegradedOwners {
    private static final ConcurrentHashMap<Object, String> BY_LOADER = new ConcurrentHashMap<>();

    private DegradedOwners() {}

    /** The legacy id of the mod whose jar (module) or class loader holds {@code cls}; else the package root of the class name; never null. */
    public static String of(Class<?> cls) {
        if (cls == null) return Degraded.OTHER;
        String mod = ofMod(cls);
        return mod != null ? mod : ofClassName(cls.getName());
    }

    /** The legacy id of the mod whose jar (module) or class loader holds {@code cls}, or null when no mod owns it (JDK, game, layer classes). */
    public static String ofMod(Class<?> cls) {
        if (cls == null) return null;
        try {
            ClassLoader loader = cls.getClassLoader();
            if (loader == null || cls.getModule() == DegradedOwners.class.getModule()) return null;
            // FML puts every mod jar in one module layer with a shared class loader, so a jar is told apart by its module
            Object key = cls.getModule().isNamed() ? cls.getModule() : loader;
            if (key == loader && loader == DegradedOwners.class.getClassLoader()) return null; // unnamed module of the layer's own loader
            String cached = BY_LOADER.get(key);
            if (cached == null) {
                for (LegacyModRecord m : LegacyMods.all()) {
                    if (m.mainClass != null && (key instanceof Module ? m.mainClass.getModule() == key : m.mainClass.getClassLoader() == key)) {
                        cached = m.legacyId;
                        break;
                    }
                }
                // a miss is not cached: mods are still being registered early on
                if (cached != null) BY_LOADER.put(key, cached);
            }
            return cached;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
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
