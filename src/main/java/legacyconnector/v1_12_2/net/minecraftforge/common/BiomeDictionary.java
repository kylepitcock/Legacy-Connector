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
        private static final Map<String, Type> byName = new java.util.LinkedHashMap<>();
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
            return byName.computeIfAbsent(name, Type::new);
        }

        public static Type fromVanilla(String name) {
            return getType(name);
        }

        /** Every type, vanilla's and the ones mods added, in the order they were made. */
        public static synchronized java.util.Collection<Type> getAll() {
            return Collections.unmodifiableCollection(new java.util.ArrayList<>(byName.values()));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static synchronized void addTypes(Biome biome, Type... types) {
        var rn = biome.getRegistryName();
        if (rn != null && "minecraft".equals(rn.getNamespace())) ensureTypes(biome);
        else SETTLED.add(biome); // typed by its mod: no guess on top
        TYPES.computeIfAbsent(biome, k -> new LinkedHashSet<>()).addAll(java.util.Arrays.asList(types));
    }

    public static synchronized Set<Type> getTypes(Biome biome) {
        ensureTypes(biome);
        return Collections.unmodifiableSet(TYPES.getOrDefault(biome, Collections.emptySet()));
    }

    public static synchronized Set<Biome> getBiomes(Type type) {
        ensureAll();
        Set<Biome> out = new LinkedHashSet<>();
        for (var e : TYPES.entrySet()) if (e.getValue().contains(type)) out.add(e.getKey());
        return out;
    }

    public static synchronized boolean hasType(Biome biome, Type type) {
        ensureTypes(biome);
        return TYPES.getOrDefault(biome, Collections.emptySet()).contains(type);
    }

    public static boolean isBiomeOfType(Biome biome, Type type) {
        return hasType(biome, type);
    }

    public static synchronized boolean areSimilar(Biome a, Biome b) {
        ensureTypes(a);
        ensureTypes(b);
        Set<Type> ta = TYPES.getOrDefault(a, Collections.emptySet());
        Set<Type> tb = TYPES.getOrDefault(b, Collections.emptySet());
        for (Type t : ta) if (tb.contains(t)) return true;
        return false;
    }

    public static synchronized boolean hasType(Biome biome) {
        ensureTypes(biome);
        return TYPES.containsKey(biome);
    }

    /** The biomes whose types were settled (vanilla table or best guess), so each is typed once. */
    private static final java.util.Set<Biome> SETTLED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static boolean settling;

    /**
     * Forge typed the vanilla biomes when it loaded and guessed the types of every other biome nobody typed the first time they were asked
     * ({@code getTypes} and the queries around it). The shim's biomes (the 1.12 vanilla ones and the mods') are typed here the same way, on first
     * use: a mod that adds its spawns "to every biome with the type X" (iterating {@code Biome.REGISTRY}) finds them.
     */
    private static void ensureTypes(Biome biome) {
        if (biome == null || settling || !SETTLED.add(biome)) return;
        settling = true;
        try {
            var name = biome.getRegistryName();
            Type[] vanilla = name != null && "minecraft".equals(name.getNamespace()) ? VANILLA.get(name.getPath()) : null;
            if (vanilla != null) TYPES.computeIfAbsent(biome, k -> new LinkedHashSet<>()).addAll(java.util.Arrays.asList(vanilla));
            else if (!TYPES.containsKey(biome) && name != null && !"minecraft".equals(name.getNamespace())) {
                try {
                    makeBestGuess(biome);
                } catch (RuntimeException | LinkageError e) {
                    // a biome whose properties cannot be read stays untyped
                }
            }
        } finally {
            settling = false;
        }
    }

    private static void ensureAll() {
        for (Biome b : Biome.REGISTRY) ensureTypes(b);
    }

    /** Forge 1.12.2's types of the vanilla biomes, by the shim's names (the lower-case field names of {@code Biomes}). */
    private static final Map<String, Type[]> VANILLA = new HashMap<>();

    private static void v(String names, Type... types) {
        for (String n : names.split(",")) VANILLA.put(n.trim(), types);
    }

    static {
        Type h = Type.HOT, c = Type.COLD, w = Type.WET, d = Type.DRY, sn = Type.SNOWY, sa = Type.SANDY, f = Type.FOREST, co = Type.CONIFEROUS,
                hl = Type.HILLS, m = Type.MOUNTAIN, sp = Type.SPARSE, de = Type.DENSE, ra = Type.RARE, ju = Type.JUNGLE, sv = Type.SAVANNA,
                pl = Type.PLAINS, me = Type.MESA, ma = Type.MAGICAL, spk = Type.SPOOKY, sw = Type.SWAMP;
        v("ocean,deep_ocean,default", Type.OCEAN);
        v("frozen_ocean", c, Type.OCEAN, sn);
        v("plains", pl);
        v("desert", h, d, sa);
        v("desert_hills", h, d, sa, hl);
        v("extreme_hills", m, hl);
        v("extreme_hills_edge", m);
        v("extreme_hills_with_trees", m, f, sp);
        v("forest", f);
        v("forest_hills", f, hl);
        v("taiga", c, co, f);
        v("taiga_hills", c, co, f, hl);
        v("swampland", w, sw);
        v("river", Type.RIVER);
        v("frozen_river", c, Type.RIVER, sn);
        v("hell", Type.NETHER, h, d);
        v("sky", Type.END, c, d);
        v("void", Type.VOID);
        v("ice_plains", c, sn, Type.WASTELAND);
        v("ice_mountains", c, sn, m);
        v("mushroom_island", Type.MUSHROOM);
        v("mushroom_island_shore", Type.BEACH, Type.MUSHROOM);
        v("beach,stone_beach", Type.BEACH);
        v("cold_beach", c, Type.BEACH, sn);
        v("jungle", h, w, de, ju);
        v("jungle_hills", h, w, de, ju, hl);
        v("jungle_edge", h, w, ju, f);
        v("birch_forest", f);
        v("birch_forest_hills", f, hl);
        v("roofed_forest", spk, de, f);
        v("cold_taiga", c, co, f, sn);
        v("cold_taiga_hills", c, co, f, sn, hl);
        v("redwood_taiga", c, co, f);
        v("redwood_taiga_hills", c, co, f, hl);
        v("savanna", h, sv, pl, sp);
        v("savanna_plateau", h, sv, hl, sp);
        v("mesa", me, sa);
        v("mesa_rock", me, sp, sa);
        v("mesa_clear_rock", me, sa);
        v("mutated_plains", pl, ma, ra);
        v("mutated_desert", h, d, sa, ra);
        v("mutated_extreme_hills", m, sp, ra);
        v("mutated_extreme_hills_with_trees", m, sp, ra);
        v("mutated_forest", f, ma, ra);
        v("mutated_taiga", c, co, f, ra);
        v("mutated_taiga_cold", c, co, f, sn, ra);
        v("mutated_swampland", w, sw, ra);
        v("mutated_ice_flats", c, sn, hl, ra);
        v("mutated_jungle", h, w, de, ju, ra);
        v("mutated_jungle_edge", h, sp, ju, f, ra);
        v("mutated_birch_forest,mutated_birch_forest_hills", f, de, hl, ra);
        v("mutated_roofed_forest", spk, de, f, m, ra);
        v("mutated_redwood_taiga,mutated_redwood_taiga_hills", c, co, f, m, ra);
        v("mutated_savanna,mutated_savanna_rock", h, d, sp, sv, m, ra);
        v("mutated_mesa,mutated_mesa_rock,mutated_mesa_clear_rock", me, sa, ra);
    }

    private static String mcc$blockPath(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        if (state == null || state.getBlock() == null || state.getBlock().getRegistryName() == null) return "";
        var name = state.getBlock().getRegistryName();
        return "minecraft".equals(name.getNamespace()) ? name.getPath() : "";
    }

    /**
     * Forge's guess of a biome's types from its properties (for biomes nobody typed): trees, height, rainfall, temperature, snow and
     * surface blocks.
     */
    public static synchronized void makeBestGuess(Biome biome) {
        if (biome == null) return;
        int trees = biome.decorator == null ? 0 : biome.decorator.treesPerChunk;
        float temp = biome.getDefaultTemperature();
        float rain = biome.getRainfall();
        float base = biome.getBaseHeight();
        float variation = biome.getHeightVariation();
        if (trees >= 3) {
            if (biome.isHighHumidity() && temp >= 0.9F) addTypes(biome, Type.JUNGLE);
            else if (!biome.isHighHumidity()) addTypes(biome, Type.FOREST);
            if (temp <= 0.2F) addTypes(biome, Type.CONIFEROUS);
        } else if (variation <= 0.3F && variation >= 0.0F) {
            if (!biome.isHighHumidity() || base >= 0.0F) addTypes(biome, Type.PLAINS);
        }
        if (rain > 0.85F) addTypes(biome, Type.WET);
        if (rain < 0.15F) addTypes(biome, Type.DRY);
        if (temp > 0.85F) addTypes(biome, Type.HOT);
        if (temp < 0.15F) addTypes(biome, Type.COLD);
        if (trees > 0 && trees < 3) addTypes(biome, Type.SPARSE);
        else if (trees >= 10) addTypes(biome, Type.DENSE);
        if (biome.isHighHumidity() && base < 0.0F && variation <= 0.3F && variation >= 0.0F) addTypes(biome, Type.SWAMP);
        if (base <= -0.5F) addTypes(biome, variation == 0.0F ? Type.RIVER : Type.OCEAN);
        if (variation >= 0.4F && variation < 1.5F) addTypes(biome, Type.HILLS);
        if (variation >= 1.5F) addTypes(biome, Type.MOUNTAIN);
        if (biome.getEnableSnow()) addTypes(biome, Type.SNOWY);
        String top = mcc$blockPath(biome.topBlock);
        if (!top.equals("sand") && temp >= 1.0F && rain < 0.2F) addTypes(biome, Type.SAVANNA);
        if (top.equals("sand")) addTypes(biome, Type.SANDY);
        else if (top.equals("mycelium")) addTypes(biome, Type.MUSHROOM);
        if (mcc$blockPath(biome.fillerBlock).equals("hardened_clay")) addTypes(biome, Type.MESA);
    }
}
