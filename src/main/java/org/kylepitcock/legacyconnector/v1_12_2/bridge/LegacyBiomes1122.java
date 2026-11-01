package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.IntSupplier;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122;
import org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaEntities1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod biomes of 1.12.2 as modern biomes (the counterpart of the 1.7.10 {@code LegacyBiomes}). Every legacy {@code Biome} registered
 * under a mod's namespace becomes the data pack biome {@code <namespace>:<name>}: its climate, colours (the legacy
 * {@code getGrassColorAtPos}, {@code getFoliageColorAtPos}, {@code getWaterColorMultiplier}, {@code getSkyColorByTemp}) and the
 * mobs of its legacy spawn lists mapped to modern entity types. It has no features and carvers: the mod's own chunk generator
 * and decorator place everything (see {@code LegacyChunkGenerator1122}). The dimension's biome source
 * ({@code LegacyBiomeSource1122}) answers these biomes, and {@link BiomeBridge1122#legacy} maps them back to the mod's object.
 */
public final class LegacyBiomes1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Biomes1122");
    /** Number of decoration steps a biome lists features for. */
    private static final int STEPS = 11;

    private LegacyBiomes1122() {}

    /** The registered mod biomes (registry order). */
    public static List<Biome> modBiomes() {
        List<Biome> out = new ArrayList<>();
        try {
            for (Biome b : Registries1122.BIOMES.getValues()) {
                if (b != null && BiomeBridge1122.isModBiome(b) && validId(BiomeBridge1122.modernId(b))) out.add(b);
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Could not list the mod biomes: {}", e.toString());
        }
        return out;
    }

    private static boolean validId(ResourceLocation id) {
        return id != null && ResourceLocation.isValidNamespace(id.getNamespace()) && ResourceLocation.isValidPath(id.getPath());
    }

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        int n = 0;
        for (Biome b : modBiomes()) {
            try {
                ResourceLocation id = BiomeBridge1122.modernId(b);
                byte[] bytes = biomeJson(b).toString().getBytes(StandardCharsets.UTF_8);
                out.put("data/" + id.getNamespace() + "/worldgen/biome/" + id.getPath() + ".json", () -> bytes);
                n++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not generate the biome {}: {}", b.getRegistryName(), e.toString());
            }
        }
        if (n > 0) LOG.info("1.12.2: generated {} mod biome(s)", n);
    }

    private static JsonObject biomeJson(Biome b) {
        JsonObject json = new JsonObject();
        float temperature = safe(b::getDefaultTemperature, 0.5F);
        json.addProperty("temperature", temperature);
        json.addProperty("downfall", Math.max(0.0F, Math.min(1.0F, safe(b::getRainfall, 0.5F))));
        boolean rain = true;
        try {
            rain = b.canRain() || b.getEnableSnow();
        } catch (RuntimeException | LinkageError e) {
            // keep rain
        }
        json.addProperty("has_precipitation", rain);
        float chance = safe(b::getSpawningChance, 0.1F);
        if (chance > 0.0F) json.addProperty("creature_spawn_probability", Math.min(0.9999999F, chance));

        JsonObject effects = new JsonObject();
        effects.addProperty("fog_color", 12638463);
        int water = safeInt(b::getWaterColorMultiplier, 0xFFFFFF) & 0xFFFFFF;
        // 1.12 water textures are blue and multiplied by this (white = unchanged); 1.21 textures are grey and tinted by it
        effects.addProperty("water_color", water == 0xFFFFFF ? 4159204 : water);
        effects.addProperty("water_fog_color", 329011);
        effects.addProperty("sky_color", safeInt(() -> b.getSkyColorByTemp(temperature), 0x78A7FF) & 0xFFFFFF);
        effects.addProperty("grass_color", safeInt(() -> b.getGrassColorAtPos(new BlockPos(0, 64, 0)), 0x91BD59) & 0xFFFFFF);
        effects.addProperty("foliage_color", safeInt(() -> b.getFoliageColorAtPos(new BlockPos(0, 64, 0)), 0x77AB2F) & 0xFFFFFF);
        json.add("effects", effects);

        json.add("spawners", spawners(b));
        json.add("spawn_costs", new JsonObject());
        JsonArray features = new JsonArray();
        for (int i = 0; i < STEPS; i++) features.add(new JsonArray());
        json.add("features", features);
        json.add("carvers", new JsonArray()); // required by the 1.21 biome codec; the mod's own generator carves
        return json;
    }

    /** The legacy spawn lists as modern spawners (mob categories keyed by their serialized names). */
    private static JsonObject spawners(Biome b) {
        Map<String, JsonArray> byCategory = new LinkedHashMap<>();
        for (EnumCreatureType type : EnumCreatureType.values()) {
            List<Biome.SpawnListEntry> list;
            try {
                list = b.getSpawnableList(type);
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            if (list == null) continue;
            for (Biome.SpawnListEntry e : List.copyOf(list)) {
                if (e == null || e.entityClass == null || e.itemWeight <= 0) continue;
                Hosts1122.Info info = Hosts1122.info(e.entityClass);
                EntityType<?> et = info != null ? info.type() : VanillaEntities1122.mcc$modernTypeOf(e.entityClass);
                if (et == null) continue;
                var category = Spawns1122.mobCategory(type);
                if (info != null) Spawns1122.adoptCategory(et, category);
                ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(et);
                int min = Math.max(1, e.minGroupCount);
                JsonObject spawn = new JsonObject();
                spawn.addProperty("type", typeId.toString());
                spawn.addProperty("weight", e.itemWeight);
                spawn.addProperty("minCount", min);
                spawn.addProperty("maxCount", Math.max(min, e.maxGroupCount));
                byCategory.computeIfAbsent(category.getName(), k -> new JsonArray()).add(spawn);
            }
        }
        JsonObject out = new JsonObject();
        byCategory.forEach(out::add);
        return out;
    }

    private static float safe(Supplier<Float> s, float fallback) {
        try {
            Float v = s.get();
            return v == null ? fallback : v;
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    private static int safeInt(IntSupplier s, int fallback) {
        try {
            return s.getAsInt();
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }
}
