package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldPresetPack;

/**
 * Legacy mod world types (Biomes O' Plenty's "BIOMESOP", ...) as 1.21 world presets, listed on the Create World
 * screen's world type button ({@link LegacyWorldPresetPack}). Each preset is the vanilla overworld with a {@link LegacyLayeredBiomeSource}
 * (the world type's biome layers over the vanilla multi-noise parameters) and its own copy of the noise settings, whose id identifies
 * worlds created with the type.
 */
public final class LegacyWorldPresets {
    private LegacyWorldPresets() {}

    /** World types registered by legacy mods. */
    public static List<WorldType> modTypes() {
        List<WorldType> out = new ArrayList<>();
        for (WorldType t : WorldType.worldTypes) if (t != null && t.mcc$owner != null && t.getCanBeCreated()) out.add(t);
        return out;
    }

    public static WorldType bySettings(ResourceLocation noiseSettings) {
        for (WorldType t : modTypes()) if (t.mcc$presetId().equals(noiseSettings)) return t;
        return null;
    }

    /** True when the type brings its own terrain generator ({@code getChunkGenerator} overridden): worlds of the type generate with the mod's. */
    public static boolean hasOwnGenerator(WorldType t) {
        try {
            return t.getClass().getMethod("getChunkGenerator", legacyconnector.v1_7_10.net.minecraft.world.World.class, String.class)
                    .getDeclaringClass() != WorldType.class;
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    private static List<LegacyWorldPresetPack.Preset> presets() {
        List<LegacyWorldPresetPack.Preset> out = new ArrayList<>();
        for (WorldType t : modTypes()) {
            JsonObject generator = new JsonObject();
            if (hasOwnGenerator(t)) {
                // The mod's terrain: the legacy chunk generator of the type's world provider (the machinery of mod dimensions, dimension 0 = the
                // overworld), biomes from the type's own chunk manager.
                JsonObject source = new JsonObject();
                source.addProperty("type", "legacyconnector:legacy");
                source.addProperty("dimension", 0);
                com.google.gson.JsonArray biomes = new com.google.gson.JsonArray();
                org.kylepitcock.legacyconnector.v1_7_10.dimension.LegacyBiomeSource.allBiomeIds().forEach(biomes::add);
                source.add("biomes", biomes);
                generator.addProperty("type", "legacyconnector:legacy");
                generator.addProperty("dimension", 0);
                generator.addProperty("world_type", t.getWorldTypeName());
                generator.add("biome_source", source);
            } else {
                // The world type's own biome source: the vanilla multi-noise parameters with its legacy biome layers choosing land biomes.
                JsonObject source = new JsonObject();
                source.addProperty("type", "legacyconnector:legacy_layers");
                source.addProperty("preset", "minecraft:overworld");
                source.addProperty("world_type", t.getWorldTypeName());
                generator.add("biome_source", source);
            }
            out.add(new LegacyWorldPresetPack.Preset(t.mcc$presetId(), t.getWorldTypeName(), t.mcc$owner, t.getTranslateName(), generator));
        }
        return out;
    }

    public static void contributeData(Map<String, Supplier<byte[]>> out) {
        LegacyWorldPresetPack.contribute(out, presets());
    }

    /** Display names: the mod's own translation of the legacy key (generator.BIOMESOP), else the type name. */
    public static void registerNames() {
        LegacyWorldPresetPack.registerNames(presets());
    }
}
