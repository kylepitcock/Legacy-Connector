package legacyconnector.v1_7_10.net.minecraftforge.common;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** Forge's overworld biome lists per climate, read by GenLayerBiome. Mods add their biomes here. */
public class BiomeManager {
    @SuppressWarnings("unchecked")
    private static final TrackedList<BiomeEntry>[] biomes = new TrackedList[BiomeType.values().length];
    public static List<BiomeGenBase> oceanBiomes = new ArrayList<>();
    public static ArrayList<BiomeGenBase> strongHoldBiomes = new ArrayList<>();
    public static ArrayList<BiomeGenBase> strongHoldBiomesBlackList = new ArrayList<>();
    private static final List<BiomeGenBase> VILLAGE = new ArrayList<>();
    private static final List<BiomeGenBase> SPAWN = new ArrayList<>();

    static {
        biomes[BiomeType.DESERT.ordinal()] = new TrackedList<>(List.of(new BiomeEntry(BiomeGenBase.desert, 30),
                new BiomeEntry(BiomeGenBase.savanna, 20), new BiomeEntry(BiomeGenBase.plains, 10)));
        biomes[BiomeType.WARM.ordinal()] = new TrackedList<>(List.of(new BiomeEntry(BiomeGenBase.forest, 10),
                new BiomeEntry(BiomeGenBase.roofedForest, 10), new BiomeEntry(BiomeGenBase.extremeHills, 10),
                new BiomeEntry(BiomeGenBase.plains, 10), new BiomeEntry(BiomeGenBase.birchForest, 10), new BiomeEntry(BiomeGenBase.swampland, 10)));
        biomes[BiomeType.COOL.ordinal()] = new TrackedList<>(List.of(new BiomeEntry(BiomeGenBase.forest, 10),
                new BiomeEntry(BiomeGenBase.extremeHills, 10), new BiomeEntry(BiomeGenBase.taiga, 10), new BiomeEntry(BiomeGenBase.plains, 10)));
        biomes[BiomeType.ICY.ordinal()] = new TrackedList<>(List.of(new BiomeEntry(BiomeGenBase.icePlains, 30), new BiomeEntry(BiomeGenBase.coldTaiga, 10)));
        oceanBiomes.add(BiomeGenBase.ocean);
        oceanBiomes.add(BiomeGenBase.deepOcean);
        oceanBiomes.add(BiomeGenBase.frozenOcean);
    }

    public static List<BiomeEntry> desertBiomes = biomes[BiomeType.DESERT.ordinal()];
    public static List<BiomeEntry> warmBiomes = biomes[BiomeType.WARM.ordinal()];
    public static List<BiomeEntry> coolBiomes = biomes[BiomeType.COOL.ordinal()];
    public static List<BiomeEntry> icyBiomes = biomes[BiomeType.ICY.ordinal()];

    public static void addVillageBiome(BiomeGenBase biome, boolean canSpawn) {
        if (canSpawn && !VILLAGE.contains(biome)) VILLAGE.add(biome);
    }

    public static void removeVillageBiome(BiomeGenBase biome) {
        VILLAGE.remove(biome);
    }

    public static void addStrongholdBiome(BiomeGenBase biome) {
        if (!strongHoldBiomes.contains(biome)) strongHoldBiomes.add(biome);
    }

    public static void removeStrongholdBiome(BiomeGenBase biome) {
        strongHoldBiomes.remove(biome);
        if (!strongHoldBiomesBlackList.contains(biome)) strongHoldBiomesBlackList.add(biome);
    }

    public static void addSpawnBiome(BiomeGenBase biome) {
        if (!SPAWN.contains(biome)) SPAWN.add(biome);
    }

    public static void removeSpawnBiome(BiomeGenBase biome) {
        SPAWN.remove(biome);
    }

    public static void addBiome(BiomeType type, BiomeEntry entry) {
        // An entry for a biome its mod failed to create (null) would break every layer that draws from the list.
        if (type != null && entry != null && entry.biome != null) biomes[type.ordinal()].add(entry);
    }

    public static void removeBiome(BiomeType type, BiomeEntry entry) {
        if (type != null) biomes[type.ordinal()].remove(entry);
    }

    public static ImmutableList<BiomeEntry> getBiomes(BiomeType type) {
        ImmutableList.Builder<BiomeEntry> out = ImmutableList.builder();
        for (BiomeEntry e : biomes[type.ordinal()]) if (e != null && e.biome != null) out.add(e);
        return out.build();
    }

    public static boolean isTypeListModded(BiomeType type) {
        return biomes[type.ordinal()].isModded();
    }

    public enum BiomeType {
        DESERT, WARM, COOL, ICY;

        public static BiomeType getType(String name) {
            for (BiomeType t : values()) if (t.name().equalsIgnoreCase(name)) return t;
            return null;
        }
    }

    public static class BiomeEntry extends WeightedRandom.Item {
        public final BiomeGenBase biome;

        public BiomeEntry(BiomeGenBase biome, int weight) {
            super(weight);
            this.biome = biome;
        }
    }

    /** List that remembers whether mods changed it. */
    private static class TrackedList<E> extends ArrayList<E> {
        private boolean modded;

        TrackedList(Collection<? extends E> c) {
            super(c);
        }

        @Override
        public boolean add(E e) {
            modded = true;
            return super.add(e);
        }

        @Override
        public boolean remove(Object o) {
            modded = true;
            return super.remove(o);
        }

        @Override
        public E remove(int index) {
            modded = true;
            return super.remove(index);
        }

        boolean isModded() {
            return modded;
        }
    }
}
