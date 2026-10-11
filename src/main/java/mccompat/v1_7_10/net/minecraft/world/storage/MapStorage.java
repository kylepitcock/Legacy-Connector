package mccompat.v1_7_10.net.minecraft.world.storage;

import java.util.HashMap;
import java.util.Map;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.world.WorldSavedData;
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

    public MapStorage(DimensionDataStorage storage) {
        this.storage = storage;
    }

    private static String file(String name) {
        return "legacy_" + name.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    /** The saved data, or null if none was saved yet (mods then create it and call {@link #setData}). */
    public WorldSavedData loadData(Class<?> type, String name) {
        WorldSavedData cached = loadedDataMap.get(name);
        if (cached != null || storage == null) return cached;
        Adapter adapter = storage.get(new SavedData.Factory<>(() -> new Adapter(create(type, name)),
                (tag, registries) -> Adapter.load(create(type, name), tag), null), file(name));
        if (adapter == null || adapter.legacy == null) return null;
        loadedDataMap.put(name, adapter.legacy);
        return adapter.legacy;
    }

    public void setData(String name, WorldSavedData data) {
        if (data == null) return;
        loadedDataMap.put(name, data);
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
            org.slf4j.LoggerFactory.getLogger("Retrofit/SavedData").warn("Cannot create legacy saved data {} ({})", name, type.getName(), e);
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
