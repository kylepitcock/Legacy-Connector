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
        if (Boolean.getBoolean("legacyconnector.mc125.clientCallbacks")) return false; // dev switch for A/B checks (read live, settable from a script)
        return !SINGLE_WORLD.isEmpty() && legacy != null && SINGLE_WORLD.contains(legacy.getClass().getClassLoader());
    }

    /**
     * {@code world.isRemote} as a 1.2.5 mod sees it (the loader rewrites the field reads of 1.2.5 jars to this): Minecraft 1.2.5 single player had one
     * world and it was not remote. The single-player client world reads as local while the integrated server's thread runs the mod (item, block and
     * tile entity callbacks that reach the world through {@code Minecraft.theWorld}).
     */
    public static boolean isRemote(legacyconnector.v1_7_10.net.minecraft.world.World world) {
        if (!world.isRemote) return false;
        if (!(world instanceof legacyconnector.v1_7_10.net.minecraft.client.multiplayer.WorldClient)) return true;
        return !onServerThread();
    }

    private static boolean onServerThread() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null && !server.isDedicatedServer() && server.isSameThread();
    }

    public static boolean active() {
        return Boolean.getBoolean(PROPERTY);
    }
}
