package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyBiomeFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy mod biomes as modern biomes. Each gets a generated biome definition: its own climate and colours, the
 * features of the closest vanilla biome (ores, caves, vegetation) and the {@code legacyconnector:legacy_biome} feature, which
 * applies the legacy surface blocks (topBlock/fillerBlock) and runs the biome's own legacy decorator.
 */
public final class LegacyBiomes {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Biomes");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    /** Decoration step "vegetal_decoration". */
    private static final int VEGETAL_DECORATION = 9;
    private static volatile Map<ResourceLocation, BiomeGenBase> byId = Map.of();
    private static boolean hooked;
    /** Chunks the legacy biome decoration ran in (diagnostics). */
    public static final java.util.concurrent.atomic.AtomicInteger DECORATED = new java.util.concurrent.atomic.AtomicInteger();

    private LegacyBiomes() {}

    /** Mod biomes with a modern id, refreshed when data is generated. */
    public static synchronized List<BiomeGenBase> modBiomes() {
        List<BiomeGenBase> out = new ArrayList<>();
        Map<ResourceLocation, BiomeGenBase> map = new HashMap<>();
        for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray()) {
            if (b == null || !b.mcc$isModBiome()) continue;
            if (map.putIfAbsent(b.mcc$modernId(), b) == null) out.add(b);
        }
        byId = map;
        if (!hooked) {
            hooked = true;
            LegacyBiomeFeature.addHook(LegacyBiomes::decorate);
        }
        return out;
    }

    public static BiomeGenBase byModernId(ResourceLocation id) {
        return byId.get(id);
    }

    // ------------------------------------------------------------------ data generation

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        int n = 0;
        for (BiomeGenBase b : modBiomes()) {
            try {
                JsonObject json = biomeJson(b);
                ResourceLocation id = b.mcc$modernId();
                byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
                out.put("data/" + id.getNamespace() + "/worldgen/biome/" + id.getPath() + ".json", () -> bytes);
                n++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not generate biome {}: {}", b, e.toString());
            }
        }
        if (n > 0) LOG.info("Generated {} legacy biome(s)", n);
        contributeTags(out);
    }

    /**
     * Vanilla structures, mob spawns and biome modifiers are keyed by biome tags ({@code #minecraft:has_structure/village_plains},
     * {@code #minecraft:is_overworld}, ...): a legacy biome joins every tag that contains the vanilla biome it is modelled on.
     */
    private static void contributeTags(Map<String, Supplier<byte[]>> out) {
        List<net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome>> keys = new ArrayList<>();
        for (Class<?> holder : new Class<?>[] {net.minecraft.tags.BiomeTags.class, net.neoforged.neoforge.common.Tags.Biomes.class}) {
            for (var f : holder.getFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) || !net.minecraft.tags.TagKey.class.isAssignableFrom(f.getType())) continue;
                try {
                    @SuppressWarnings("unchecked")
                    var key = (net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome>) f.get(null);
                    if (key.registry().equals(net.minecraft.core.registries.Registries.BIOME)) keys.add(key);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // not a readable constant
                }
            }
        }
        Map<String, java.util.Set<String>> memberCache = new HashMap<>();
        Map<String, List<String>> membership = new HashMap<>();
        for (BiomeGenBase b : modBiomes()) {
            String template = "minecraft:" + templateFor(b);
            for (var key : keys) {
                if (!resolveTag(key.location(), memberCache, new java.util.HashSet<>()).contains(template)) continue;
                membership.computeIfAbsent("data/" + key.location().getNamespace() + "/tags/worldgen/biome/" + key.location().getPath() + ".json", k -> new ArrayList<>())
                        .add(b.mcc$modernId().toString());
            }
        }
        for (var e : membership.entrySet()) {
            JsonObject json = new JsonObject();
            json.addProperty("replace", false);
            JsonArray values = new JsonArray();
            e.getValue().forEach(values::add);
            json.add("values", values);
            byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
            out.put(e.getKey(), () -> bytes);
        }
        if (!membership.isEmpty()) LOG.info("Legacy biomes joined {} biome tag(s)", membership.size());
    }

    /** The biome ids a data-pack biome tag contains (nested tags resolved), read from the vanilla and NeoForge jars. */
    private static java.util.Set<String> resolveTag(ResourceLocation tag, Map<String, java.util.Set<String>> cache, java.util.Set<String> visiting) {
        String id = tag.toString();
        var cached = cache.get(id);
        if (cached != null) return cached;
        java.util.Set<String> members = new java.util.HashSet<>();
        if (!visiting.add(id)) return members;
        String path = "data/" + tag.getNamespace() + "/tags/worldgen/biome/" + tag.getPath() + ".json";
        for (ClassLoader cl : new ClassLoader[] {net.minecraft.world.level.biome.Biome.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
            if (cl == null) continue;
            try (InputStream in = cl.getResourceAsStream(path)) {
                if (in == null) continue;
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (JsonElement v : json.getAsJsonArray("values")) {
                    String value = v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString();
                    if (value.startsWith("#")) members.addAll(resolveTag(ResourceLocation.parse(value.substring(1)), cache, visiting));
                    else members.add(value);
                }
            } catch (Exception ignored) {
                // try the next loader
            }
        }
        cache.put(id, members);
        return members;
    }

    private static JsonObject biomeJson(BiomeGenBase b) {
        JsonObject json = template(templateFor(b));
        json.addProperty("temperature", b.temperature);
        json.addProperty("downfall", Math.max(0.0F, Math.min(1.0F, b.rainfall)));
        json.addProperty("has_precipitation", b.getEnableSnow() || b.canSpawnLightningBolt());
        JsonObject effects = json.getAsJsonObject("effects");
        if (effects == null) {
            effects = new JsonObject();
            json.add("effects", effects);
        }
        int grass = safeColor(() -> b.getBiomeGrassColor(0, 64, 0), 0x91BD59);
        int foliage = safeColor(() -> b.getBiomeFoliageColor(0, 64, 0), 0x77AB2F);
        if (grass != 0x91BD59) effects.addProperty("grass_color", grass & 0xFFFFFF);
        if (foliage != 0x77AB2F) effects.addProperty("foliage_color", foliage & 0xFFFFFF);
        if ((b.waterColorMultiplier & 0xFFFFFF) != 0xFFFFFF) effects.addProperty("water_color", b.waterColorMultiplier & 0xFFFFFF);
        effects.addProperty("sky_color", safeColor(() -> b.getSkyColorByTemp(b.temperature), 0x78A7FF) & 0xFFFFFF);
        JsonArray steps = json.getAsJsonArray("features");
        if (steps == null) {
            steps = new JsonArray();
            json.add("features", steps);
        }
        while (steps.size() <= VEGETAL_DECORATION) steps.add(new JsonArray());
        // Trees and grass come from the legacy decorator (the biome's own generators); keep the small plants.
        JsonArray vegetation = new JsonArray();
        for (JsonElement f : steps.get(VEGETAL_DECORATION).getAsJsonArray()) {
            String id = f.isJsonPrimitive() ? f.getAsString() : "";
            if (id.contains("tree") || id.contains("grass") || id.contains("bamboo") || id.contains("vines")) continue;
            vegetation.add(f);
        }
        vegetation.add("legacyconnector:legacy_biome");
        steps.set(VEGETAL_DECORATION, vegetation);
        return json;
    }

    private static int safeColor(Supplier<Integer> s, int fallback) {
        try {
            return s.get();
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    /** Closest vanilla biome, whose generation features the legacy biome inherits. */
    static String templateFor(BiomeGenBase b) {
        float t = b.temperature;
        float r = b.rainfall;
        int trees = b.theBiomeDecorator == null ? 0 : b.theBiomeDecorator.treesPerChunk;
        if (b.getEnableSnow() || t < 0.15F) return trees > 0 ? "snowy_taiga" : "snowy_plains";
        if (t >= 1.0F && r <= 0.2F) return b.topBlock == Blocks.sand ? "desert" : "savanna";
        if (t >= 0.95F && r >= 0.8F) return "jungle";
        if (r >= 0.85F && t >= 0.5F) return "swamp";
        if (t <= 0.3F) return trees > 0 ? "taiga" : "plains";
        return trees >= 2 ? "forest" : "plains";
    }

    private static JsonObject template(String name) {
        String path = "data/minecraft/worldgen/biome/" + name + ".json";
        for (ClassLoader cl : new ClassLoader[] {net.minecraft.world.level.biome.Biome.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
            if (cl == null) continue;
            try (InputStream in = cl.getResourceAsStream(path)) {
                if (in != null) return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception ignored) {
                // try the next loader
            }
        }
        LOG.warn("Vanilla biome template {} not found; generating a bare biome", name);
        JsonObject json = new JsonObject();
        JsonObject effects = new JsonObject();
        effects.addProperty("fog_color", 12638463);
        effects.addProperty("water_color", 4159204);
        effects.addProperty("water_fog_color", 329011);
        json.add("effects", effects);
        JsonObject spawners = new JsonObject();
        json.add("spawners", spawners);
        json.add("spawn_costs", new JsonObject());
        JsonObject carvers = new JsonObject();
        carvers.add("air", GSON.toJsonTree(List.of("minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon")));
        json.add("carvers", carvers);
        JsonArray features = new JsonArray();
        for (int i = 0; i < 11; i++) features.add(new JsonArray());
        json.add("features", features);
        return json;
    }

    // ------------------------------------------------------------------ decoration

    private static boolean decorate(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        BlockPos center = new BlockPos((chunkX << 4) + 8, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, (chunkX << 4) + 8, (chunkZ << 4) + 8), (chunkZ << 4) + 8);
        BiomeGenBase legacy = level.getBiome(center).unwrapKey().map(k -> byModernId(k.location())).orElse(null);
        if (legacy == null) return false;
        DECORATED.incrementAndGet();
        applySurface(level, chunkX, chunkZ, legacy);
        // 1.7.10 seeded each chunk's decoration from the world seed and chunk position.
        Random rand = new Random(level.getSeed());
        long a = rand.nextLong() / 2L * 2L + 1L;
        long b = rand.nextLong() / 2L * 2L + 1L;
        rand.setSeed(chunkX * a + chunkZ * b ^ level.getSeed());
        legacy.decorate(World.mcc$wrap(level), rand, chunkX << 4, chunkZ << 4);
        return true;
    }

    /** The legacy biome's topBlock/fillerBlock replace grass/dirt in its columns (what genTerrainBlocks did). */
    private static void applySurface(WorldGenLevel level, int chunkX, int chunkZ, BiomeGenBase legacy) {
        Block top = legacy.topBlock;
        Block filler = legacy.fillerBlock;
        if ((top == null || top == Blocks.grass) && (filler == null || filler == Blocks.dirt)) return;
        BlockState topState = top == null ? null : BlockBridge.toModern(top, legacy.field_150604_aj);
        BlockState fillerState = filler == null ? null : BlockBridge.toModern(filler, 0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = (chunkX << 4) + dx;
                int z = (chunkZ << 4) + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                pos.set(x, y, z);
                if (level.getBiome(pos).unwrapKey().map(k -> byModernId(k.location()) != legacy).orElse(true)) continue;
                for (int depth = 0; depth < 4; depth++, pos.move(0, -1, 0)) {
                    BlockState s = level.getBlockState(pos);
                    if (depth == 0 && topState != null && s.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        level.setBlock(pos, topState, 2);
                    } else if (fillerState != null && (s.is(net.minecraft.world.level.block.Blocks.DIRT) || s.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK))) {
                        level.setBlock(pos, fillerState, 2);
                    } else if (depth > 0) {
                        break;
                    }
                }
            }
        }
    }
}
