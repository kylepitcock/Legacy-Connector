package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import net.minecraft.resources.ResourceLocation;

/**
 * Modern biome -> the 1.12.2 biome legacy code sees there: a vanilla biome as its 1.12 counterpart (the {@code Biomes} constant of
 * the closest 1.12 biome), a biome whose id a legacy mod registered as itself. Lookups are cached (mods sample biomes per block).
 */
public final class BiomeBridge1122 {
    private BiomeBridge1122() {}

    /** modern vanilla biome path -> 1.12 Biomes field. */
    private static final Map<String, String> VANILLA = new HashMap<>();
    private static final Map<ResourceLocation, Biome> CACHE = new ConcurrentHashMap<>();

    private static void map(String legacyField, String... modern) {
        for (String m : modern) VANILLA.put(m, legacyField);
    }

    static {
        map("OCEAN", "ocean", "warm_ocean", "lukewarm_ocean", "cold_ocean");
        map("DEEP_OCEAN", "deep_ocean", "deep_lukewarm_ocean", "deep_cold_ocean");
        map("FROZEN_OCEAN", "frozen_ocean", "deep_frozen_ocean");
        map("PLAINS", "plains", "meadow");
        map("MUTATED_PLAINS", "sunflower_plains");
        map("DESERT", "desert");
        map("EXTREME_HILLS", "windswept_hills", "stony_peaks");
        map("EXTREME_HILLS_WITH_TREES", "windswept_forest");
        map("MUTATED_EXTREME_HILLS", "windswept_gravelly_hills");
        map("FOREST", "forest");
        map("MUTATED_FOREST", "flower_forest");
        map("TAIGA", "taiga", "grove");
        map("REDWOOD_TAIGA", "old_growth_pine_taiga");
        map("MUTATED_REDWOOD_TAIGA", "old_growth_spruce_taiga");
        map("COLD_TAIGA", "snowy_taiga");
        map("SWAMPLAND", "swamp", "mangrove_swamp");
        map("RIVER", "river");
        map("FROZEN_RIVER", "frozen_river");
        map("HELL", "nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas");
        map("SKY", "the_end", "end_highlands", "end_midlands", "small_end_islands", "end_barrens");
        map("VOID", "the_void");
        map("ICE_PLAINS", "snowy_plains", "snowy_slopes");
        map("MUTATED_ICE_FLATS", "ice_spikes");
        map("ICE_MOUNTAINS", "frozen_peaks", "jagged_peaks");
        map("MUSHROOM_ISLAND", "mushroom_fields");
        map("BEACH", "beach");
        map("COLD_BEACH", "snowy_beach");
        map("STONE_BEACH", "stony_shore");
        map("JUNGLE", "jungle");
        map("JUNGLE_EDGE", "sparse_jungle");
        map("MUTATED_JUNGLE", "bamboo_jungle");
        map("BIRCH_FOREST", "birch_forest");
        map("MUTATED_BIRCH_FOREST", "old_growth_birch_forest");
        map("ROOFED_FOREST", "dark_forest");
        map("SAVANNA", "savanna");
        map("SAVANNA_PLATEAU", "savanna_plateau");
        map("MUTATED_SAVANNA", "windswept_savanna");
        map("MESA", "badlands");
        map("MESA_ROCK", "wooded_badlands");
        map("MUTATED_MESA", "eroded_badlands");
        map("FOREST_HILLS", "cherry_grove");
        map("PLAINS", "dripstone_caves", "lush_caves", "deep_dark");
    }

    /** The legacy biome at a modern biome (plains when there is none). */
    public static Biome legacy(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder) {
        ResourceLocation key = holder == null ? null : holder.unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
        return legacy(key);
    }

    public static Biome legacy(ResourceLocation key) {
        if (key == null) return fallback();
        Biome cached = CACHE.get(key);
        if (cached != null) return cached;
        Biome found = null;
        try {
            found = Registries1122.BIOMES.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(key.getNamespace(), key.getPath()));
            if ("minecraft".equals(key.getNamespace())) {
                String field = VANILLA.get(key.getPath());
                Biome vanilla = field == null ? null : (Biome) legacyconnector.v1_12_2.net.minecraft.init.Biomes.class.getField(field).get(null);
                if (vanilla != null) found = vanilla;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // plains
        }
        if (found == null) found = fallback();
        if (found != null) CACHE.put(key, found);
        return found;
    }

    private static Biome fallback() {
        return legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS;
    }
}
