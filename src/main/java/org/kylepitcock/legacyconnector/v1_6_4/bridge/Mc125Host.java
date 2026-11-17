package org.kylepitcock.legacyconnector.v1_6_4.bridge;

/**
 * Whether Minecraft 1.2.5 hosting is on in this session: the loader sets the system property when it accepts a 1.2.5 jar (toggle
 * {@code experimental.minecraft125}, see {@code Mc125Jars.enabled()} in the loader, the one place that decides). Run-time code that only
 * 1.2.5 mods need but that sits on a path every version uses asks this, so with the toggle off it stays inert.
 */
public final class Mc125Host {
    public static final String PROPERTY = "legacyconnector.mc125.active";

    private Mc125Host() {}

    private static final java.util.Set<ClassLoader> SINGLE_WORLD = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** A 1.2.5 mod's class loader: Minecraft 1.2.5 single player had ONE world, so its item/entity logic must run once (on the server side here). */
    public static void registerSingleWorld(ClassLoader loader) {
        if (loader != null) SINGLE_WORLD.add(loader);
    }

    /** Whether this legacy object's class is from a 1.2.5 mod (its callbacks run only on the logical server, not again on the client world). */
    public static boolean singleWorld(Object legacy) {
        return !SINGLE_WORLD.isEmpty() && legacy != null && SINGLE_WORLD.contains(legacy.getClass().getClassLoader());
    }

    public static boolean active() {
        return Boolean.getBoolean(PROPERTY);
    }
}
