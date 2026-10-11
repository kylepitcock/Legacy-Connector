package mccompat.v1_7_10.net.minecraftforge.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraft.world.WorldProvider;
import mccompat.v1_7_10.net.minecraft.world.WorldServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Forge's dimension registry. Provider types and dimension ids that mods register are recorded (mods probe for free
 * ids and loop until registration succeeds); the vanilla dimensions 0, -1 and 1 are the running server's levels.
 * Worlds for mod-registered dimensions are not created.
 */
public class DimensionManager {
    private static final Map<Integer, Class<?>> PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> SPAWN_LOADED = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> DIMENSIONS = new TreeMap<>();

    static {
        PROVIDERS.put(0, WorldProvider.class);
        PROVIDERS.put(-1, WorldProvider.class);
        PROVIDERS.put(1, WorldProvider.class);
        DIMENSIONS.put(0, 0);
        DIMENSIONS.put(-1, -1);
        DIMENSIONS.put(1, 1);
    }

    public static boolean registerProviderType(int id, Class<?> provider, boolean keepLoaded) {
        if (PROVIDERS.putIfAbsent(id, provider) != null) return false;
        SPAWN_LOADED.put(id, keepLoaded);
        return true;
    }

    public static int[] unregisterProviderType(int providerId) {
        PROVIDERS.remove(providerId);
        List<Integer> dims = new ArrayList<>();
        synchronized (DIMENSIONS) {
            DIMENSIONS.entrySet().removeIf(e -> {
                if (e.getValue() != providerId) return false;
                dims.add(e.getKey());
                return true;
            });
        }
        return dims.stream().mapToInt(Integer::intValue).toArray();
    }

    public static void init() {
    }

    public static void registerDimension(int id, int providerType) {
        if (!PROVIDERS.containsKey(providerType)) {
            throw new IllegalArgumentException(String.format("Failed to register dimension for id %d, provider type %d does not exist", id, providerType));
        }
        synchronized (DIMENSIONS) {
            if (DIMENSIONS.containsKey(id)) {
                throw new IllegalArgumentException(String.format("Failed to register dimension for id %d, One is already registered", id));
            }
            DIMENSIONS.put(id, providerType);
        }
    }

    public static void unregisterDimension(int id) {
        synchronized (DIMENSIONS) {
            if (!DIMENSIONS.containsKey(id)) {
                throw new IllegalArgumentException(String.format("Failed to unregister dimension for id %d; No provider registered", id));
            }
            DIMENSIONS.remove(id);
        }
    }

    public static boolean isDimensionRegistered(int dim) {
        synchronized (DIMENSIONS) {
            return DIMENSIONS.containsKey(dim);
        }
    }

    public static int getProviderType(int dim) {
        synchronized (DIMENSIONS) {
            Integer type = DIMENSIONS.get(dim);
            if (type == null) throw new IllegalArgumentException(String.format("Could not get provider type for dimension %d, does not exist", dim));
            return type;
        }
    }

    public static WorldProvider getProvider(int dim) {
        WorldServer w = getWorld(dim);
        return w == null ? null : w.provider;
    }

    public static Integer[] getIDs() {
        List<Integer> ids = new ArrayList<>();
        for (WorldServer w : getWorlds()) ids.add(w.provider.dimensionId);
        return ids.toArray(Integer[]::new);
    }

    public static Integer[] getStaticDimensionIDs() {
        synchronized (DIMENSIONS) {
            return DIMENSIONS.keySet().toArray(Integer[]::new);
        }
    }

    public static int getNextFreeDimId() {
        synchronized (DIMENSIONS) {
            int next = 0;
            while (DIMENSIONS.containsKey(next)) next++;
            return next;
        }
    }

    public static WorldServer getWorld(int id) {
        for (WorldServer w : getWorlds()) if (w.provider != null && w.provider.dimensionId == id) return w;
        return null;
    }

    public static WorldServer[] getWorlds() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return new WorldServer[0];
        List<WorldServer> out = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (World.mcc$wrap(level) instanceof WorldServer w) out.add(w);
        }
        return out.toArray(WorldServer[]::new);
    }

    public static void initDimension(int dim) {
    }

    public static void unloadWorld(int id) {
    }

    public static WorldProvider createProviderFor(int dim) {
        WorldServer w = getWorld(dim);
        return w == null ? null : w.provider;
    }

    public static File getCurrentSaveRootDirectory() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getWorldPath(LevelResource.ROOT).toFile();
    }

    public static boolean shouldLoadSpawn(int dim) {
        return SPAWN_LOADED.getOrDefault(dim, dim == 0);
    }
}
