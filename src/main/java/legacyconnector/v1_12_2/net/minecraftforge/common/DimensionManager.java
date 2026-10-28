package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.util.Map;
import java.util.TreeMap;
import legacyconnector.v1_12_2.net.minecraft.world.DimensionType;

/**
 * Forge's dimension id table: which numeric dimension has which {@link DimensionType}. The vanilla three are registered from the
 * start; mod dimensions are recorded for the dimension bridge (1.12 world providers are not generated yet).
 */
public class DimensionManager {
    private static final Map<Integer, DimensionType> DIMENSIONS = new TreeMap<>();
    private static final java.util.Set<Integer> KEEP_LOADED = new java.util.HashSet<>();

    static {
        DIMENSIONS.put(0, DimensionType.OVERWORLD);
        DIMENSIONS.put(-1, DimensionType.NETHER);
        DIMENSIONS.put(1, DimensionType.THE_END);
    }

    public static synchronized void registerDimension(int id, DimensionType type) {
        if (type == null) throw new IllegalArgumentException("Failed to register dimension for id " + id + ": no type");
        if (DIMENSIONS.containsKey(id)) {
            // Forge threw here. Earlier Legacy Connector builds answered getNextFreeDimId() with 0, and mods saved that id in
            // their configs (AE2's storageDimensionID=0): keep the first registration rather than disable the mod.
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").warn("Dimension id {} is already registered ({}); ignoring {} (fix the id in the mod's config)",
                    id, DIMENSIONS.get(id).getName(), type.getName());
            return;
        }
        DIMENSIONS.put(id, type);
    }

    public static synchronized void unregisterDimension(int id) {
        if (!DIMENSIONS.containsKey(id)) throw new IllegalArgumentException("Failed to unregister dimension for id " + id + "; no provider registered");
        DIMENSIONS.remove(id);
    }

    public static synchronized boolean isDimensionRegistered(int dim) {
        return DIMENSIONS.containsKey(dim);
    }

    public static synchronized DimensionType getProviderType(int dim) {
        if (!DIMENSIONS.containsKey(dim)) throw new IllegalArgumentException("Could not get provider type for dimension " + dim + ", does not exist");
        return DIMENSIONS.get(dim);
    }

    public static synchronized Integer[] getIDs() {
        return DIMENSIONS.keySet().toArray(new Integer[0]);
    }

    public static synchronized Integer[] getStaticDimensionIDs() {
        return getIDs();
    }

    public static synchronized int[] getDimensions(DimensionType type) {
        return DIMENSIONS.entrySet().stream().filter(e -> e.getValue() == type).mapToInt(Map.Entry::getKey).toArray();
    }

    public static synchronized int getNextFreeDimId() {
        int next = 0;
        while (DIMENSIONS.containsKey(next)) next++;
        return next;
    }

    public static synchronized boolean keepDimensionLoaded(int dim, boolean keep) {
        if (keep) KEEP_LOADED.add(dim);
        else KEEP_LOADED.remove(dim);
        return true;
    }

    public static synchronized boolean shouldLoadSpawn(int dim) {
        DimensionType t = DIMENSIONS.get(dim);
        return KEEP_LOADED.contains(dim) || t != null && t.shouldLoadSpawn();
    }

    // ------------------------------------------------------------------ modern levels

    /** Numeric ids of modern dimensions that are not vanilla's: first come, first numbered, from 2 up (skipping registered ids). */
    private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Integer> MODERN_IDS = new java.util.HashMap<>();

    /** The legacy dimension id of a modern level key. */
    public static synchronized int mcc$idOf(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key) {
        if (key == null || key == net.minecraft.world.level.Level.OVERWORLD) return 0;
        if (key == net.minecraft.world.level.Level.NETHER) return -1;
        if (key == net.minecraft.world.level.Level.END) return 1;
        Integer known = MODERN_IDS.get(key);
        if (known != null) return known;
        // a stable id across restarts when possible: the levels the server has, in key order
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            java.util.List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> keys = new java.util.ArrayList<>(server.levelKeys());
            keys.sort(java.util.Comparator.comparing(k -> k.location().toString()));
            for (var k : keys) {
                if (k == net.minecraft.world.level.Level.OVERWORLD || k == net.minecraft.world.level.Level.NETHER || k == net.minecraft.world.level.Level.END
                        || MODERN_IDS.containsKey(k)) continue;
                MODERN_IDS.put(k, nextModernId());
            }
            known = MODERN_IDS.get(key);
            if (known != null) return known;
        }
        int id = nextModernId();
        MODERN_IDS.put(key, id);
        return id;
    }

    private static int nextModernId() {
        int id = 2;
        while (DIMENSIONS.containsKey(id) || MODERN_IDS.containsValue(id)) id++;
        return id;
    }

    /** The modern level of a legacy dimension id, or null. */
    public static net.minecraft.server.level.ServerLevel mcc$level(int id) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        if (id == 0) return server.overworld();
        for (var level : server.getAllLevels()) {
            if (mcc$idOf(level.dimension()) == id) return level;
        }
        return null;
    }

    public static legacyconnector.v1_12_2.net.minecraft.world.WorldServer getWorld(int id) {
        var level = mcc$level(id);
        return level == null ? null : (legacyconnector.v1_12_2.net.minecraft.world.WorldServer) org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level);
    }

    /** The world, created if needed: modern levels exist from server start, so this is {@link #getWorld}. */
    public static legacyconnector.v1_12_2.net.minecraft.world.WorldServer getWorld(int id, boolean resetUnloadDelay) {
        return getWorld(id);
    }

    public static legacyconnector.v1_12_2.net.minecraft.world.WorldServer[] getWorlds() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return new legacyconnector.v1_12_2.net.minecraft.world.WorldServer[0];
        java.util.List<legacyconnector.v1_12_2.net.minecraft.world.WorldServer> out = new java.util.ArrayList<>();
        for (var level : server.getAllLevels()) {
            out.add((legacyconnector.v1_12_2.net.minecraft.world.WorldServer) org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level));
        }
        return out.toArray(new legacyconnector.v1_12_2.net.minecraft.world.WorldServer[0]);
    }

    public static legacyconnector.v1_12_2.net.minecraft.world.WorldProvider getProvider(int id) {
        var world = getWorld(id);
        return world == null ? null : world.getProvider();
    }

    /** A new provider of the dimension's type (Forge: for a dimension that has no world yet). */
    public static legacyconnector.v1_12_2.net.minecraft.world.WorldProvider createProviderFor(int dim) {
        DimensionType type;
        synchronized (DimensionManager.class) {
            type = DIMENSIONS.get(dim);
        }
        if (type == null) throw new RuntimeException("No WorldProvider bound for dimension " + dim);
        Class<?> cls = type.mcc$providerClass();
        legacyconnector.v1_12_2.net.minecraft.world.WorldProvider provider;
        try {
            provider = cls == null ? (dim == -1 ? new legacyconnector.v1_12_2.net.minecraft.world.WorldProviderHell()
                    : dim == 1 ? new legacyconnector.v1_12_2.net.minecraft.world.WorldProviderEnd() : new legacyconnector.v1_12_2.net.minecraft.world.WorldProviderSurface())
                    : (legacyconnector.v1_12_2.net.minecraft.world.WorldProvider) cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create provider for dimension " + dim, e);
        }
        provider.setDimension(dim);
        return provider;
    }

    /** Modern levels are created by the server; a legacy dimension without one has no world to start. */
    public static void initDimension(int dim) {
    }

    public static void unloadWorld(int id) {
    }

    public static java.io.File getCurrentSaveRootDirectory() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
    }

    public static boolean isDimensionLoaded(int dim) {
        return mcc$level(dim) != null;
    }

    /** Every registered dimension type with the ids registered for it. */
    public static synchronized java.util.Map<DimensionType, it.unimi.dsi.fastutil.ints.IntSortedSet> getRegisteredDimensions() {
        java.util.Map<DimensionType, it.unimi.dsi.fastutil.ints.IntSortedSet> out = new java.util.LinkedHashMap<>();
        DIMENSIONS.forEach((id, type) -> out.computeIfAbsent(type, t -> new it.unimi.dsi.fastutil.ints.IntRBTreeSet()).add((int) id));
        return out;
    }

    /** The modern server creates and unloads its levels itself; a legacy world handed in here is not adopted. */
    public static void setWorld(int id, legacyconnector.v1_12_2.net.minecraft.world.WorldServer world, legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer server) {
    }
}
