package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;

/** Biome tags: which types a biome has. */
public class BiomeDictionary {
    private static final Map<Biome, Set<Type>> TYPES = new LinkedHashMap<>();

    /** A biome type (HOT, FOREST, ...); mods can create their own. */
    public static final class Type {
        private static final Map<String, Type> BY_NAME = new java.util.LinkedHashMap<>();
        private final String name;

        public static final Type HOT = getType("HOT");
        public static final Type COLD = getType("COLD");
        public static final Type SPARSE = getType("SPARSE");
        public static final Type DENSE = getType("DENSE");
        public static final Type WET = getType("WET");
        public static final Type DRY = getType("DRY");
        public static final Type SAVANNA = getType("SAVANNA");
        public static final Type CONIFEROUS = getType("CONIFEROUS");
        public static final Type JUNGLE = getType("JUNGLE");
        public static final Type SPOOKY = getType("SPOOKY");
        public static final Type DEAD = getType("DEAD");
        public static final Type LUSH = getType("LUSH");
        public static final Type NETHER = getType("NETHER");
        public static final Type END = getType("END");
        public static final Type MUSHROOM = getType("MUSHROOM");
        public static final Type MAGICAL = getType("MAGICAL");
        public static final Type RARE = getType("RARE");
        public static final Type OCEAN = getType("OCEAN");
        public static final Type RIVER = getType("RIVER");
        public static final Type WATER = getType("WATER");
        public static final Type MESA = getType("MESA");
        public static final Type FOREST = getType("FOREST");
        public static final Type PLAINS = getType("PLAINS");
        public static final Type MOUNTAIN = getType("MOUNTAIN");
        public static final Type HILLS = getType("HILLS");
        public static final Type SWAMP = getType("SWAMP");
        public static final Type SANDY = getType("SANDY");
        public static final Type SNOWY = getType("SNOWY");
        public static final Type WASTELAND = getType("WASTELAND");
        public static final Type BEACH = getType("BEACH");
        public static final Type VOID = getType("VOID");

        private Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static synchronized Type getType(String name, Type... subTypes) {
            return BY_NAME.computeIfAbsent(name, Type::new);
        }

        public static Type fromVanilla(String name) {
            return getType(name);
        }

        /** Every type, vanilla's and the ones mods added, in the order they were made. */
        public static synchronized java.util.Collection<Type> getAll() {
            return Collections.unmodifiableCollection(new java.util.ArrayList<>(BY_NAME.values()));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static synchronized void addTypes(Biome biome, Type... types) {
        TYPES.computeIfAbsent(biome, k -> new LinkedHashSet<>()).addAll(java.util.Arrays.asList(types));
    }

    public static synchronized Set<Type> getTypes(Biome biome) {
        return Collections.unmodifiableSet(TYPES.getOrDefault(biome, Collections.emptySet()));
    }

    public static synchronized Set<Biome> getBiomes(Type type) {
        Set<Biome> out = new LinkedHashSet<>();
        for (var e : TYPES.entrySet()) if (e.getValue().contains(type)) out.add(e.getKey());
        return out;
    }

    public static synchronized boolean hasType(Biome biome, Type type) {
        return TYPES.getOrDefault(biome, Collections.emptySet()).contains(type);
    }

    public static boolean isBiomeOfType(Biome biome, Type type) {
        return hasType(biome, type);
    }

    public static synchronized boolean areSimilar(Biome a, Biome b) {
        Set<Type> ta = TYPES.getOrDefault(a, Collections.emptySet());
        Set<Type> tb = TYPES.getOrDefault(b, Collections.emptySet());
        for (Type t : ta) if (tb.contains(t)) return true;
        return false;
    }

    public static synchronized boolean hasType(Biome biome) {
        return TYPES.containsKey(biome);
    }
}
