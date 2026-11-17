package legacyconnector.v1_7_10.net.minecraft.world.storage;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.WorldSavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Legacy saved-data registry over a modern {@link DimensionDataStorage}: each legacy {@link WorldSavedData} is stored
 * as {@code data/legacy_<name>.dat} in the world. Without a storage (client side) data lives in memory only.
 */
public class MapStorage {
    private final DimensionDataStorage storage;
    private final Map<String, WorldSavedData> loadedDataMap = new HashMap<>();
    /** The loaded data as a list (1.7.10 kept both; access transformers and reflection of mods read it). */
    public final java.util.List<WorldSavedData> loadedDataList = new java.util.ArrayList<>();

    public MapStorage(DimensionDataStorage storage) {
        this.storage = storage;
        migrateLegacyFiles(storage);
    }

    /**
     * Mods that pass the world's save handler (Mystcraft reads its saved ages this way at server start, before any world asks): the
     * running server's own data folder; in memory only without a server (client side).
     */
    public MapStorage(ISaveHandler handler) {
        this(serverStorage(handler));
    }

    private static DimensionDataStorage serverStorage(ISaveHandler handler) {
        if (handler == null) return null;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        var overworld = server == null ? null : server.overworld();
        if (overworld == null) return null;
        // only the save handler of the server's world (a client world's handler points nowhere)
        java.io.File dir = handler.getWorldDirectory();
        java.io.File root = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        try {
            if (dir == null || !dir.getCanonicalFile().equals(root.getCanonicalFile())) return null;
        } catch (java.io.IOException e) {
            return null;
        }
        return overworld.getDataStorage();
    }

    /**
     * The file of the saved data {@code name}: {@code data/<name>.dat}, as 1.7.10 named it. Mods find their data by listing that folder
     * (Mystcraft lists {@code agedata_<id>.dat} at server start to register its ages), so the name must stay the mod's own. Names the
     * modern game keeps for its own data (maps, id counters, scoreboard, raids ...) and names with odd characters keep the {@code legacy_} prefix.
     */
    private static String file(String name) {
        String clean = name.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return reserved(clean) || !clean.equals(name) ? "legacy_" + clean : clean;
    }

    private static boolean reserved(String name) {
        return name.startsWith("legacy_") || name.equals("idcounts") || name.equals("scoreboard") || name.equals("chunks") || name.startsWith("raids")
                || name.equals("random_sequences") || name.startsWith("command_storage_") || name.matches("map_-?\\d+")
                || name.startsWith("minecraft") || name.startsWith("neoforge");
    }

    private static final java.util.Set<java.nio.file.Path> MIGRATED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Earlier builds saved every legacy data file as {@code legacy_<name>.dat}: renames those to the mod's own name (once per folder)
     * so a mod that lists its files finds them. A file under the new name wins.
     */
    private static void migrateLegacyFiles(DimensionDataStorage storage) {
        if (storage == null) return;
        try {
            java.nio.file.Path folder = null;
            for (var f : DimensionDataStorage.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() == java.io.File.class) {
                    f.setAccessible(true);
                    folder = ((java.io.File) f.get(storage)).toPath();
                } else if (f.getType() == java.nio.file.Path.class) {
                    f.setAccessible(true);
                    folder = (java.nio.file.Path) f.get(storage);
                }
                if (folder != null) break;
            }
            if (folder == null || !java.nio.file.Files.isDirectory(folder) || !MIGRATED.add(folder.toAbsolutePath().normalize())) return;
            try (var files = java.nio.file.Files.list(folder)) {
                for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) files::iterator) {
                    String n = p.getFileName().toString();
                    if (!n.startsWith("legacy_") || !n.endsWith(".dat")) continue;
                    String name = n.substring("legacy_".length(), n.length() - 4);
                    if (name.isEmpty() || reserved(name) || !name.equals(name.replaceAll("[^a-zA-Z0-9_.-]", "_"))) continue;
                    java.nio.file.Path target = folder.resolve(name + ".dat");
                    if (!java.nio.file.Files.exists(target)) java.nio.file.Files.move(p, target);
                }
            }
        } catch (Exception | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/SavedData").warn("Could not rename the legacy saved data files ({})", e.toString());
        }
    }

    /** The saved data, or null if none was saved yet (mods then create it and call {@link #setData}). */
    public WorldSavedData loadData(Class<?> type, String name) {
        WorldSavedData cached = loadedDataMap.get(name);
        if (cached != null || storage == null) return cached;
        Adapter adapter = storage.get(new SavedData.Factory<>(() -> new Adapter(create(type, name)),
                (tag, registries) -> Adapter.load(create(type, name), tag), null), file(name));
        if (adapter == null || adapter.legacy == null) return null;
        loadedDataMap.put(name, adapter.legacy);
        loadedDataList.add(adapter.legacy);
        return adapter.legacy;
    }

    public void setData(String name, WorldSavedData data) {
        if (data == null) return;
        WorldSavedData previous = loadedDataMap.put(name, data);
        if (previous != null) loadedDataList.remove(previous);
        loadedDataList.add(data);
        if (storage != null) storage.set(file(name), new Adapter(data));
    }

    public void saveAllData() {
        if (storage != null) storage.save();
    }

    public int getUniqueDataId(String key) {
        WorldSavedData counts = loadData(IdCounts.class, "idcounts");
        if (counts == null) {
            counts = new IdCounts("idcounts");
            setData("idcounts", counts);
        }
        return ((IdCounts) counts).next(key);
    }

    private static WorldSavedData create(Class<?> type, String name) {
        try {
            var ctor = type.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return (WorldSavedData) ctor.newInstance(name);
        } catch (ReflectiveOperationException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/SavedData").warn("Cannot create legacy saved data {} ({})", name, type.getName(), e);
            return null;
        }
    }

    /** Modern saved data holding a legacy instance. */
    private static final class Adapter extends SavedData {
        final WorldSavedData legacy;

        Adapter(WorldSavedData legacy) {
            this.legacy = legacy;
        }

        static Adapter load(WorldSavedData legacy, CompoundTag tag) {
            if (legacy != null) legacy.readFromNBT(new NBTTagCompound(tag.getCompound("data")));
            return new Adapter(legacy);
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            if (legacy != null) {
                NBTTagCompound data = new NBTTagCompound();
                legacy.writeToNBT(data);
                tag.put("data", data.mcc$modern());
                legacy.setDirty(false);
            }
            return tag;
        }

        @Override
        public boolean isDirty() {
            return legacy != null && legacy.isDirty() || super.isDirty();
        }
    }

    /** Legacy "idcounts" (map ids and similar counters). */
    public static final class IdCounts extends WorldSavedData {
        private final Map<String, Integer> counts = new HashMap<>();

        public IdCounts(String name) {
            super(name);
        }

        int next(String key) {
            int id = counts.getOrDefault(key, -1) + 1;
            counts.put(key, id);
            markDirty();
            return id;
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            for (String k : tag.getKeySet()) counts.put(k, tag.getInteger(k));
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            counts.forEach(tag::setInteger);
        }
    }
}
