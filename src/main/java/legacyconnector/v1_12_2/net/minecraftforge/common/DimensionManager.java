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

    public static synchronized Integer[] getDimensions(DimensionType type) {
        return DIMENSIONS.entrySet().stream().filter(e -> e.getValue() == type).map(Map.Entry::getKey).toArray(Integer[]::new);
    }

    public static synchronized int getNextFreeDimId() {
        int next = 0;
        while (DIMENSIONS.containsKey(next)) next++;
        return next;
    }

    public static synchronized void keepDimensionLoaded(int dim, boolean keep) {
        if (keep) KEEP_LOADED.add(dim);
        else KEEP_LOADED.remove(dim);
    }

    public static synchronized boolean shouldLoadSpawn(int dim) {
        DimensionType t = DIMENSIONS.get(dim);
        return KEEP_LOADED.contains(dim) || t != null && t.shouldLoadSpawn();
    }
}
