package legacyconnector.v1_12_2.net.minecraftforge.common;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.init.Biomes;
import legacyconnector.v1_12_2.net.minecraft.util.WeightedRandom;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider;

/**
 * Forge's overworld biome lists per climate (vanilla's defaults plus what mods add), read by {@code GenLayerBiome}; the spawn biomes are
 * {@code BiomeProvider.allowedBiomes} as in 1.12. Village and stronghold biomes are kept as data (the modern game places its own structures).
 */
public class BiomeManager {
    private static TrackedList<BiomeEntry>[] biomes;
    public static List<Biome> oceanBiomes = new ArrayList<>();
    public static ArrayList<Biome> strongHoldBiomes = new ArrayList<>();
    public static ArrayList<Biome> strongHoldBiomesBlackList = new ArrayList<>();
    private static final List<Biome> VILLAGE = new ArrayList<>();

    static {
        oceanBiomes.add(Biomes.OCEAN);
        oceanBiomes.add(Biomes.DEEP_OCEAN);
        oceanBiomes.add(Biomes.FROZEN_OCEAN);
    }

    /** Vanilla's lists, made on first use (the vanilla biome wrappers need the modern registries). */
    @SuppressWarnings("unchecked")
    private static synchronized TrackedList<BiomeEntry>[] lists() {
        if (biomes == null) {
            TrackedList<BiomeEntry>[] b = new TrackedList[BiomeType.values().length];
            for (int i = 0; i < b.length; i++) b[i] = new TrackedList<>(List.of());
            b[BiomeType.DESERT.ordinal()].addVanilla(List.of(new BiomeEntry(Biomes.DESERT, 30), new BiomeEntry(Biomes.SAVANNA, 20),
                    new BiomeEntry(Biomes.PLAINS, 10)));
            b[BiomeType.WARM.ordinal()].addVanilla(List.of(new BiomeEntry(Biomes.FOREST, 10), new BiomeEntry(Biomes.ROOFED_FOREST, 10),
                    new BiomeEntry(Biomes.EXTREME_HILLS, 10), new BiomeEntry(Biomes.PLAINS, 10), new BiomeEntry(Biomes.BIRCH_FOREST, 10),
                    new BiomeEntry(Biomes.SWAMPLAND, 10)));
            b[BiomeType.COOL.ordinal()].addVanilla(List.of(new BiomeEntry(Biomes.FOREST, 10), new BiomeEntry(Biomes.EXTREME_HILLS, 10),
                    new BiomeEntry(Biomes.TAIGA, 10), new BiomeEntry(Biomes.PLAINS, 10)));
            b[BiomeType.ICY.ordinal()].addVanilla(List.of(new BiomeEntry(Biomes.ICE_PLAINS, 30), new BiomeEntry(Biomes.COLD_TAIGA, 10)));
            biomes = b;
        }
        return biomes;
    }

    public static void addVillageBiome(Biome biome, boolean canSpawn) {
        synchronized (VILLAGE) {
            if (canSpawn && biome != null && !VILLAGE.contains(biome)) VILLAGE.add(biome);
        }
    }

    public static void removeVillageBiome(Biome biome) {
        synchronized (VILLAGE) {
            VILLAGE.remove(biome);
        }
    }

    public static void addStrongholdBiome(Biome biome) {
        if (biome != null && !strongHoldBiomes.contains(biome)) {
            strongHoldBiomes.add(biome);
            strongHoldBiomesBlackList.remove(biome);
        }
    }

    public static void removeStrongholdBiome(Biome biome) {
        strongHoldBiomes.remove(biome);
        if (biome != null && !strongHoldBiomesBlackList.contains(biome)) strongHoldBiomesBlackList.add(biome);
    }

    public static void addSpawnBiome(Biome biome) {
        if (biome != null && !BiomeProvider.allowedBiomes.contains(biome)) BiomeProvider.allowedBiomes.add(biome);
    }

    public static void removeSpawnBiome(Biome biome) {
        BiomeProvider.allowedBiomes.remove(biome);
    }

    public static void addBiome(BiomeType type, BiomeEntry entry) {
        // an entry for a biome its mod failed to create (null) would break every layer that draws from the list
        if (type == null || entry == null || entry.biome == null) return;
        synchronized (BiomeManager.class) {
            lists()[type.ordinal()].add(entry);
        }
    }

    public static void removeBiome(BiomeType type, BiomeEntry entry) {
        if (type == null) return;
        synchronized (BiomeManager.class) {
            lists()[type.ordinal()].remove(entry);
        }
    }

    public static ImmutableList<BiomeEntry> getBiomes(BiomeType type) {
        if (type == null) return null;
        synchronized (BiomeManager.class) {
            return ImmutableList.copyOf(lists()[type.ordinal()]);
        }
    }

    public static boolean isTypeListModded(BiomeType type) {
        if (type == null) return false;
        synchronized (BiomeManager.class) {
            return lists()[type.ordinal()].isModded();
        }
    }

    /** The entries mods added to a climate list (GenLayerBiome adds them to its own copy of vanilla's defaults). */
    public static List<BiomeEntry> mcc$modEntries(String type) {
        BiomeType t = BiomeType.getType(type);
        if (t == null) return new ArrayList<>();
        synchronized (BiomeManager.class) {
            List<BiomeEntry> out = new ArrayList<>();
            TrackedList<BiomeEntry> list = lists()[t.ordinal()];
            for (BiomeEntry e : list) if (!list.vanilla.contains(e)) out.add(e);
            return out;
        }
    }

    public enum BiomeType {
        DESERT, WARM, COOL, ICY;

        /** The type of that name; 1.12 added an unknown name as a new constant, here an unknown name is null. */
        public static BiomeType getType(String name) {
            if (name == null) return null;
            for (BiomeType t : values()) if (t.name().equalsIgnoreCase(name)) return t;
            return null;
        }
    }

    public static class BiomeEntry extends WeightedRandom.Item {
        public final Biome biome;

        public BiomeEntry(Biome biome, int weight) {
            super(weight);
            this.biome = biome;
        }
    }

    /** A list that remembers whether mods changed it. */
    private static class TrackedList<E> extends ArrayList<E> {
        private boolean modded;
        final List<E> vanilla = new ArrayList<>();

        TrackedList(Collection<? extends E> c) {
            super(c);
        }

        void addVanilla(Collection<? extends E> c) {
            super.addAll(c);
            vanilla.addAll(c);
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
