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
                if (b != null && BiomeBridge1122.isModBiome(b) && validId(BiomeBridge1122.modernId(b))) {
                    out.add(b);
                    // the name maps and JourneyMap show: Biome.getName() is the translation key biome.<namespace>.<path>
                    String name = b.getBiomeName();
                    var id = BiomeBridge1122.modernId(b);
                    if (name != null && !name.isBlank()) {
                        org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations.put("en_us", "biome." + id.getNamespace() + "." + id.getPath(), name);
                    }
                }
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
        // biomes the default overworld places (BiomeManager lists): they keep their analog's vanilla features and carvers and decorate through the legacy decorator
        Map<ResourceLocation, String> placed = new java.util.HashMap<>();
        try {
            for (var e : LegacyBiomeClimate1122.entries()) placed.put(e.biome().location(), e.analog().location().getPath());
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Could not read the overworld biome placement: {}", e.toString());
        }
        for (Biome b : modBiomes()) {
            try {
                ResourceLocation id = BiomeBridge1122.modernId(b);
                JsonObject biome = biomeJson(b);
                if (placed.containsKey(id)) inheritAnalog(biome, placed.get(id));
                byte[] bytes = biome.toString().getBytes(StandardCharsets.UTF_8);
                out.put("data/" + id.getNamespace() + "/worldgen/biome/" + id.getPath() + ".json", () -> bytes);
                n++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not generate the biome {}: {}", b.getRegistryName(), e.toString());
            }
        }
        if (n > 0) LOG.info("1.12.2: generated {} mod biome(s)", n);
    }

    /**
     * A mod biome that generates in the default overworld: the vanilla analog's features (ores, caves' carvers, plants) with its
     * trees and grass replaced by the legacy decorator ({@code legacyconnector:legacy_biome}, which also applies the surface blocks).
     */
    private static void inheritAnalog(JsonObject json, String analog) {
        JsonObject template = null;
        String path = "data/minecraft/worldgen/biome/" + analog + ".json";
        for (ClassLoader cl : new ClassLoader[] {net.minecraft.world.level.biome.Biome.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
            if (cl == null) continue;
            try (var in = cl.getResourceAsStream(path)) {
                if (in != null) {
                    template = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    break;
                }
            } catch (java.io.IOException | RuntimeException ignored) {
                // try the next loader
            }
        }
        if (template == null) {
            LOG.warn("Vanilla biome template {} not found; the mod biome keeps no vanilla features", analog);
            return;
        }
        JsonArray steps = template.getAsJsonArray("features");
        while (steps.size() <= 9) steps.add(new JsonArray());
        JsonArray vegetation = new JsonArray();
        for (var f : steps.get(9).getAsJsonArray()) {
            String id = f.isJsonPrimitive() ? f.getAsString() : "";
            if (id.contains("tree") || id.contains("grass") || id.contains("bamboo") || id.contains("vines")) continue;
            vegetation.add(f);
        }
        vegetation.add("legacyconnector:legacy_biome");
        steps.set(9, vegetation);
        json.add("features", steps);
        if (template.has("carvers")) json.add("carvers", template.get("carvers"));
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
        var carvers = new com.google.gson.JsonObject();
        carvers.add("air", new JsonArray());
        json.add("carvers", carvers); // required by the 1.21 biome codec; the mod's own generator carves
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

    // ------------------------------------------------------------------ decoration in the default overworld

    private static final java.util.concurrent.atomic.AtomicInteger DECORATED = new java.util.concurrent.atomic.AtomicInteger();
    private static volatile Map<ResourceLocation, Biome> byId;

    public static void registerDecoration() {
        org.kylepitcock.legacyconnector.core.worldgen.LegacyBiomeFeature.addHook(LegacyBiomes1122::decorate);
    }

    /** O(1): chunk generation asks for this per column; the registries are final once the first chunk generates, so the map is built once. */
    private static Biome byModernId(ResourceLocation id) {
        Map<ResourceLocation, Biome> m = byId;
        if (m == null) {
            synchronized (LegacyBiomes1122.class) {
                m = byId;
                if (m == null) {
                    Map<ResourceLocation, Biome> built = new java.util.HashMap<>();
                    for (Biome b : modBiomes()) built.put(BiomeBridge1122.modernId(b), b);
                    byId = m = built;
                }
            }
        }
        return m.get(id);
    }

    /** What GenLayer-placed mod biomes got in 1.12: the surface blocks of genTerrainBlocks, then the biome's decorator for the chunk. */
    private static boolean decorate(net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> ctx) {
        net.minecraft.world.level.WorldGenLevel level = ctx.level();
        net.minecraft.core.BlockPos origin = ctx.origin();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        int cx = (chunkX << 4) + 8;
        int cz = (chunkZ << 4) + 8;
        var center = new net.minecraft.core.BlockPos(cx, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, cx, cz), cz);
        Biome legacy = level.getBiome(center).unwrapKey().map(k -> byModernId(k.location())).orElse(null);
        if (legacy == null) return false;
        if (DECORATED.incrementAndGet() <= 5) LOG.info("1.12.2 overworld decoration of chunk {} {} in {}", chunkX, chunkZ, BiomeBridge1122.modernId(legacy));
        applySurface(level, chunkX, chunkZ, legacy);
        legacyconnector.v1_12_2.net.minecraft.world.World world = WorldBridge1122.wrap(level);
        legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk.mcc$beginPopulating(chunkX, chunkZ);
        try {
            // ChunkGeneratorOverworld.populate's seeding
            java.util.Random rand = new java.util.Random(level.getSeed());
            long a = rand.nextLong() / 2L * 2L + 1L;
            long b = rand.nextLong() / 2L * 2L + 1L;
            rand.setSeed((long) chunkX * a + (long) chunkZ * b ^ level.getSeed());
            legacy.decorate(world, rand, new BlockPos(chunkX << 4, 0, chunkZ << 4));
        } finally {
            legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk.mcc$endPopulating();
            WorldBridge1122.mcc$releaseRegion();
        }
        return true;
    }

    /** The legacy biome's topBlock/fillerBlock replace the vanilla grass/dirt in its columns (what genTerrainBlocks did). */
    private static void applySurface(net.minecraft.world.level.WorldGenLevel level, int chunkX, int chunkZ, Biome legacy) {
        var top = legacy.topBlock == null ? null : StatesBridge1122.toModern(legacy.topBlock);
        var filler = legacy.fillerBlock == null ? null : StatesBridge1122.toModern(legacy.fillerBlock);
        var grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK;
        var dirt = net.minecraft.world.level.block.Blocks.DIRT;
        if ((top == null || top.is(grass)) && (filler == null || filler.is(dirt))) return;
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = (chunkX << 4) + dx;
                int z = (chunkZ << 4) + dz;
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                pos.set(x, y, z);
                if (level.getBiome(pos).unwrapKey().map(k -> byModernId(k.location()) != legacy).orElse(true)) continue;
                for (int depth = 0; depth < 4; depth++, pos.move(0, -1, 0)) {
                    var s = level.getBlockState(pos);
                    if (depth == 0 && top != null && s.is(grass)) {
                        level.setBlock(pos, top, 2);
                    } else if (filler != null && (s.is(dirt) || s.is(grass))) {
                        level.setBlock(pos, filler, 2);
                    } else if (depth > 0) {
                        break;
                    }
                }
            }
        }
    }
}
