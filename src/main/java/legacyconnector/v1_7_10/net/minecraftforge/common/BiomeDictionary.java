package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** Forge's biome type tags (FOREST, HOT, MAGICAL...), with the vanilla biomes pre-tagged as in Forge 1.7.10. */
public class BiomeDictionary {
    public enum Type {
        HOT, COLD, SPARSE, DENSE, WET, DRY, SAVANNA, CONIFEROUS, JUNGLE, SPOOKY, DEAD, LUSH, NETHER, END, MUSHROOM, MAGICAL,
        OCEAN, RIVER, WATER(OCEAN, RIVER), MESA, FOREST, PLAINS, MOUNTAIN, HILLS, SWAMP, SANDY, SNOWY, WASTELAND, BEACH,
        DESERT(SANDY), FROZEN(SNOWY);

        private final List<Type> subTags;

        Type(Type... subTags) {
            this.subTags = List.of(subTags);
        }

        public static Type getType(String name, Type... subTypes) {
            for (Type t : values()) if (t.name().equalsIgnoreCase(name)) return t;
            return null;
        }
    }

    private static final Map<BiomeGenBase, EnumSet<Type>> TYPES = new HashMap<>();

    static {
        registerVanilla();
    }

    public static synchronized boolean registerBiomeType(BiomeGenBase biome, Type... types) {
        if (biome == null) return false;
        EnumSet<Type> set = TYPES.computeIfAbsent(biome, b -> EnumSet.noneOf(Type.class));
        for (Type t : types) {
            set.add(t);
            set.addAll(t.subTags);
        }
        return true;
    }

    public static synchronized BiomeGenBase[] getBiomesForType(Type type) {
        List<BiomeGenBase> out = new ArrayList<>();
        TYPES.forEach((b, set) -> {
            if (has(set, type)) out.add(b);
        });
        return out.toArray(new BiomeGenBase[0]);
    }

    public static synchronized Type[] getTypesForBiome(BiomeGenBase biome) {
        EnumSet<Type> set = TYPES.get(biome);
        if (set == null) {
            makeBestGuess(biome);
            set = TYPES.getOrDefault(biome, EnumSet.noneOf(Type.class));
        }
        return set.toArray(new Type[0]);
    }

    public static boolean areBiomesEquivalent(BiomeGenBase a, BiomeGenBase b) {
        for (Type t : getTypesForBiome(a)) if (isBiomeOfType(b, t)) return true;
        return false;
    }

    /** As Forge's: a biome nobody registered gets the heuristic guess on first use (mod biomes, biomes the layer maps from modern ones). */
    public static synchronized boolean isBiomeOfType(BiomeGenBase biome, Type type) {
        if (biome == null) return false;
        EnumSet<Type> set = TYPES.get(biome);
        if (set == null) {
            makeBestGuess(biome);
            set = TYPES.get(biome);
        }
        return set != null && has(set, type);
    }

    /**
     * Whether a type set holds {@code type}. The 1.6.4 types the 1.7.10 API dropped are derived from the 1.7.10 ones, so 1.6.4 mods
     * (which test {@code DESERT}, {@code FROZEN}, {@code WATER}) find the biomes Forge 1.6.4 tagged with them: the vanilla ones are
     * registered explicitly, other biomes (modern ones mapped in, mods') by what they are.
     */
    private static boolean has(EnumSet<Type> set, Type type) {
        if (set.contains(type)) return true;
        return switch (type) {
            case WATER -> set.contains(Type.OCEAN) || set.contains(Type.RIVER);
            case DESERT -> set.contains(Type.SANDY) && (set.contains(Type.HOT) || set.contains(Type.DRY)) && !set.contains(Type.MESA) && !set.contains(Type.BEACH);
            case FROZEN -> set.contains(Type.SNOWY) && !set.contains(Type.FOREST) && !set.contains(Type.CONIFEROUS) && !set.contains(Type.BEACH);
            default -> false;
        };
    }

    public static synchronized boolean isBiomeRegistered(BiomeGenBase biome) {
        return TYPES.containsKey(biome);
    }

    public static boolean isBiomeRegistered(int id) {
        return isBiomeRegistered(BiomeGenBase.mcc$biomeOrOcean(id));
    }

    public static void registerAllBiomes() {
        for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray()) if (b != null && !isBiomeRegistered(b)) makeBestGuess(b);
    }

    public static void registerAllBiomesAndGenerateEvents() {
        registerAllBiomes();
    }

    /** Forge's heuristic tagging from temperature, rainfall and decoration counts. */
    public static void makeBestGuess(BiomeGenBase biome) {
        if (biome == null) return;
        if (biome.theBiomeDecorator != null && biome.theBiomeDecorator.treesPerChunk >= 3) {
            registerBiomeType(biome, biome.temperature >= 0.9F ? Type.JUNGLE : biome.temperature <= 0.25F ? Type.CONIFEROUS : Type.FOREST);
        } else if (biome.heightVariation <= 0.3F && biome.heightVariation >= 0.0F) {
            registerBiomeType(biome, Type.PLAINS);
        }
        if (biome.rainfall > 0.85F) registerBiomeType(biome, Type.WET);
        if (biome.rainfall < 0.15F) registerBiomeType(biome, Type.DRY);
        if (biome.temperature > 0.85F) registerBiomeType(biome, Type.HOT);
        if (biome.temperature < 0.15F) registerBiomeType(biome, Type.COLD);
        if (biome.rootHeight > 1.0F) registerBiomeType(biome, Type.MOUNTAIN);
        if (biome.rootHeight < 0.0F) registerBiomeType(biome, Type.OCEAN);
        if (!isBiomeRegistered(biome)) registerBiomeType(biome);
    }

    private static void registerVanilla() {
        registerBiomeType(BiomeGenBase.ocean, Type.OCEAN);
        registerBiomeType(BiomeGenBase.plains, Type.PLAINS);
        registerBiomeType(BiomeGenBase.desert, Type.HOT, Type.DRY, Type.SANDY, Type.DESERT);
        registerBiomeType(BiomeGenBase.extremeHills, Type.MOUNTAIN, Type.HILLS);
        registerBiomeType(BiomeGenBase.forest, Type.FOREST);
        registerBiomeType(BiomeGenBase.taiga, Type.COLD, Type.CONIFEROUS, Type.FOREST);
        registerBiomeType(BiomeGenBase.swampland, Type.WET, Type.SWAMP);
        registerBiomeType(BiomeGenBase.river, Type.RIVER);
        registerBiomeType(BiomeGenBase.hell, Type.NETHER, Type.HOT, Type.DRY);
        registerBiomeType(BiomeGenBase.sky, Type.END, Type.COLD, Type.DRY);
        registerBiomeType(BiomeGenBase.frozenOcean, Type.COLD, Type.OCEAN, Type.SNOWY, Type.FROZEN);
        registerBiomeType(BiomeGenBase.frozenRiver, Type.COLD, Type.RIVER, Type.SNOWY, Type.FROZEN);
        registerBiomeType(BiomeGenBase.icePlains, Type.COLD, Type.SNOWY, Type.WASTELAND, Type.FROZEN);
        registerBiomeType(BiomeGenBase.iceMountains, Type.COLD, Type.SNOWY, Type.MOUNTAIN, Type.FROZEN);
        registerBiomeType(BiomeGenBase.mushroomIsland, Type.MUSHROOM);
        registerBiomeType(BiomeGenBase.mushroomIslandShore, Type.MUSHROOM, Type.BEACH);
        registerBiomeType(BiomeGenBase.beach, Type.BEACH);
        registerBiomeType(BiomeGenBase.desertHills, Type.HOT, Type.DRY, Type.SANDY, Type.HILLS, Type.DESERT);
        registerBiomeType(BiomeGenBase.forestHills, Type.FOREST, Type.HILLS);
        registerBiomeType(BiomeGenBase.taigaHills, Type.COLD, Type.CONIFEROUS, Type.FOREST, Type.HILLS);
        registerBiomeType(BiomeGenBase.extremeHillsEdge, Type.MOUNTAIN);
        registerBiomeType(BiomeGenBase.jungle, Type.HOT, Type.WET, Type.DENSE, Type.JUNGLE);
        registerBiomeType(BiomeGenBase.jungleHills, Type.HOT, Type.WET, Type.DENSE, Type.JUNGLE, Type.HILLS);
        registerBiomeType(BiomeGenBase.jungleEdge, Type.HOT, Type.WET, Type.JUNGLE, Type.FOREST);
        registerBiomeType(BiomeGenBase.deepOcean, Type.OCEAN);
        registerBiomeType(BiomeGenBase.stoneBeach, Type.BEACH);
        registerBiomeType(BiomeGenBase.coldBeach, Type.COLD, Type.BEACH, Type.SNOWY);
        registerBiomeType(BiomeGenBase.birchForest, Type.FOREST);
        registerBiomeType(BiomeGenBase.birchForestHills, Type.FOREST, Type.HILLS);
        registerBiomeType(BiomeGenBase.roofedForest, Type.SPOOKY, Type.DENSE, Type.FOREST);
        registerBiomeType(BiomeGenBase.coldTaiga, Type.COLD, Type.CONIFEROUS, Type.FOREST, Type.SNOWY);
        registerBiomeType(BiomeGenBase.coldTaigaHills, Type.COLD, Type.CONIFEROUS, Type.FOREST, Type.SNOWY, Type.HILLS);
        registerBiomeType(BiomeGenBase.megaTaiga, Type.COLD, Type.CONIFEROUS, Type.FOREST);
        registerBiomeType(BiomeGenBase.megaTaigaHills, Type.COLD, Type.CONIFEROUS, Type.FOREST, Type.HILLS);
        registerBiomeType(BiomeGenBase.extremeHillsPlus, Type.MOUNTAIN, Type.FOREST, Type.SPARSE);
        registerBiomeType(BiomeGenBase.savanna, Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE);
        registerBiomeType(BiomeGenBase.savannaPlateau, Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE);
        registerBiomeType(BiomeGenBase.mesa, Type.MESA, Type.SANDY);
        registerBiomeType(BiomeGenBase.mesaPlateau_F, Type.MESA, Type.SPARSE, Type.SANDY);
        registerBiomeType(BiomeGenBase.mesaPlateau, Type.MESA, Type.SANDY);
    }
}
