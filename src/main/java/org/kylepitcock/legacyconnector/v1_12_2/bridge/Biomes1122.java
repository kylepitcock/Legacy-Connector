package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

/**
 * Modern biomes as 1.12.2 biomes. A vanilla biome is the matching {@code Biomes} constant (1.13+ renamed and split them:
 * windswept_hills is EXTREME_HILLS, the new ocean and mountain variants map to their closest 1.12 biome); a biome a legacy mod
 * registered is that mod's object; any other biome (native mods) gets one legacy view per key. Each legacy biome answers its
 * climate (temperature, rainfall, snow) from the modern biome it was resolved from. Lookups are cached per key.
 */
public final class Biomes1122 {
    private static final Map<String, String> VANILLA = new java.util.HashMap<>();
    private static final Map<ResourceKey<net.minecraft.world.level.biome.Biome>, Biome> CACHE = new ConcurrentHashMap<>();
    /** Legacy field -> the modern biome whose climate it answers with (the first listed: windswept_hills for EXTREME_HILLS). */
    private static final Map<String, String> CANONICAL = new java.util.HashMap<>();

    static {
        String[][] pairs = {
                {"ocean", "OCEAN"}, {"plains", "PLAINS"}, {"desert", "DESERT"}, {"windswept_hills", "EXTREME_HILLS"}, {"forest", "FOREST"},
                {"taiga", "TAIGA"}, {"swamp", "SWAMPLAND"}, {"river", "RIVER"}, {"nether_wastes", "HELL"}, {"soul_sand_valley", "HELL"},
                {"crimson_forest", "HELL"}, {"warped_forest", "HELL"}, {"basalt_deltas", "HELL"}, {"the_end", "SKY"}, {"small_end_islands", "SKY"},
                {"end_midlands", "SKY"}, {"end_highlands", "SKY"}, {"end_barrens", "SKY"}, {"frozen_ocean", "FROZEN_OCEAN"},
                {"deep_frozen_ocean", "FROZEN_OCEAN"}, {"frozen_river", "FROZEN_RIVER"}, {"snowy_plains", "ICE_PLAINS"},
                {"mushroom_fields", "MUSHROOM_ISLAND"}, {"beach", "BEACH"}, {"jungle", "JUNGLE"}, {"bamboo_jungle", "JUNGLE"},
                {"sparse_jungle", "JUNGLE_EDGE"}, {"deep_ocean", "DEEP_OCEAN"}, {"deep_lukewarm_ocean", "DEEP_OCEAN"},
                {"deep_cold_ocean", "DEEP_OCEAN"}, {"warm_ocean", "OCEAN"}, {"lukewarm_ocean", "OCEAN"}, {"cold_ocean", "OCEAN"},
                {"stony_shore", "STONE_BEACH"}, {"snowy_beach", "COLD_BEACH"}, {"birch_forest", "BIRCH_FOREST"}, {"dark_forest", "ROOFED_FOREST"},
                {"snowy_taiga", "COLD_TAIGA"}, {"grove", "COLD_TAIGA"}, {"old_growth_pine_taiga", "REDWOOD_TAIGA"},
                {"old_growth_spruce_taiga", "MUTATED_REDWOOD_TAIGA"}, {"windswept_forest", "EXTREME_HILLS_WITH_TREES"}, {"savanna", "SAVANNA"},
                {"savanna_plateau", "SAVANNA_PLATEAU"}, {"windswept_savanna", "MUTATED_SAVANNA"}, {"badlands", "MESA"},
                {"wooded_badlands", "MESA_ROCK"}, {"eroded_badlands", "MUTATED_MESA"}, {"the_void", "VOID"}, {"sunflower_plains", "MUTATED_PLAINS"},
                {"windswept_gravelly_hills", "MUTATED_EXTREME_HILLS"}, {"flower_forest", "MUTATED_FOREST"}, {"ice_spikes", "MUTATED_ICE_FLATS"},
                {"old_growth_birch_forest", "MUTATED_BIRCH_FOREST"}, {"meadow", "PLAINS"}, {"cherry_grove", "FOREST"}, {"snowy_slopes", "ICE_MOUNTAINS"},
                {"frozen_peaks", "ICE_MOUNTAINS"}, {"jagged_peaks", "ICE_MOUNTAINS"}, {"stony_peaks", "EXTREME_HILLS"},
                {"mangrove_swamp", "MUTATED_SWAMPLAND"}, {"dripstone_caves", "PLAINS"}, {"lush_caves", "JUNGLE"}, {"deep_dark", "PLAINS"},
        };
        for (String[] p : pairs) {
            VANILLA.put(p[0], p[1]);
            CANONICAL.putIfAbsent(p[1], p[0]);
        }
    }

    private Biomes1122() {}

    /** The legacy biome of a modern one (never null: PLAINS when the holder is unbound). */
    public static Biome legacy(Holder<net.minecraft.world.level.biome.Biome> holder) {
        if (holder == null) return plains();
        var key = holder.unwrapKey().orElse(null);
        if (key == null) return plains();
        Biome cached = CACHE.get(key);
        if (cached != null) return cached;
        Biome b = resolve(key);
        if (b == null) b = plains();
        String field = "minecraft".equals(key.location().getNamespace()) ? VANILLA.get(key.location().getPath()) : null;
        if (field == null || key.location().getPath().equals(CANONICAL.get(field))) b.mcc$bind(holder.value());
        CACHE.put(key, b);
        return b;
    }

    private static Biome resolve(ResourceKey<net.minecraft.world.level.biome.Biome> key) {
        var id = key.location();
        if ("minecraft".equals(id.getNamespace())) {
            String field = VANILLA.get(id.getPath());
            if (field != null) {
                Biome b = constant(field);
                if (b != null) return b;
            }
        }
        // a legacy mod's own biome under the same name
        Biome mod = Registries1122.BIOMES.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(id.getNamespace(), id.getPath()));
        if (mod != null) return mod;
        Biome view = new Biome(new Biome.BiomeProperties(id.getPath()));
        view.setRegistryName(id.getNamespace(), id.getPath());
        return view;
    }

    /** A {@code Biomes} constant by field name, or null. */
    public static Biome constant(String field) {
        try {
            return (Biome) legacyconnector.v1_12_2.net.minecraft.init.Biomes.class.getField(field).get(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static Biome plains() {
        return legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS;
    }

    /** The modern biome holder a legacy biome stands for in a level's registry, or null (a legacy mod's own biome). */
    public static Holder<net.minecraft.world.level.biome.Biome> modern(net.minecraft.core.RegistryAccess access, Biome legacy) {
        if (legacy == null || access == null) return null;
        var registry = access.registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        for (var e : CACHE.entrySet()) {
            if (e.getValue() == legacy) {
                var h = registry.getHolder(e.getKey());
                if (h.isPresent()) return h.get();
            }
        }
        for (var e : VANILLA.entrySet()) {
            if (constant(e.getValue()) == legacy) {
                var h = registry.getHolder(ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
                        net.minecraft.resources.ResourceLocation.withDefaultNamespace(e.getKey())));
                if (h.isPresent()) return h.get();
            }
        }
        return null;
    }
}
