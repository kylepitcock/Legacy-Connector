package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mccompat.v1_7_10.net.minecraft.world.WorldType;
import mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import mccompat.v1_7_10.net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.kylepitcock.mccompat.core.worldgen.BiomeOverride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Places legacy mod biomes in the overworld using the legacy biome layers. Modern terrain, oceans, rivers, beaches,
 * mountains and cave biomes are kept; where the legacy layer stack (of the mod's world type if a mod registered one,
 * e.g. Biomes O' Plenty's, else the default type with Forge BiomeManager entries) puts a mod biome, it replaces the
 * ordinary land biome at that spot.
 */
public final class LegacyBiomeLayers implements BiomeOverride.Provider {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Biomes");
    private static final int REGION = 32;
    private static final Set<ResourceKey<Biome>> KEEP = Set.of(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK,
            Biomes.MUSHROOM_FIELDS, Biomes.STONY_SHORE);
    private static boolean registered;

    private final MinecraftServer server;
    private final BiomeSource overworld;
    private final WorldType type;
    private final long seed;
    private volatile GenLayer layer;
    private final Map<Integer, Holder<Biome>> biomes;
    private final Map<Long, int[]> regions = new ConcurrentHashMap<>();

    private LegacyBiomeLayers(MinecraftServer server, BiomeSource overworld, WorldType type, long seed, Map<Integer, Holder<Biome>> biomes) {
        this.server = server;
        this.overworld = overworld;
        this.type = type;
        this.seed = seed;
        this.biomes = biomes;
    }

    /**
     * The world type's own chunk manager builds the layers when the overworld exists (Biomes O' Plenty assembles its
     * layer stack there, not in getBiomeLayer); without a level, the plain legacy stack for the type is used.
     */
    private GenLayer layer() {
        GenLayer l = layer;
        if (l != null) return l;
        synchronized (this) {
            if (layer != null) return layer;
            var level = server.overworld();
            if (level != null) {
                try {
                    var manager = type.getChunkManager(mccompat.v1_7_10.net.minecraft.world.World.mcc$wrap(level));
                    if (manager != null && manager.genBiomes != null) l = manager.genBiomes;
                } catch (RuntimeException | LinkageError e) {
                    LOG.warn("{} chunk manager failed; using its biome layers directly: {}", type.getWorldTypeName(), e.toString());
                }
            }
            if (l == null) l = GenLayer.initializeAllBiomeGenerators(seed, type)[0];
            if (level != null) { // before the overworld exists, keep retrying the full path
                layer = l;
                regions.clear();
            }
            return l;
        }
    }

    /** Called once after legacy loading. */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(LegacyBiomeLayers::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> BiomeOverride.set(null));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> logSample(e.getServer()));
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        BiomeOverride.set(null);
        MinecraftServer server = event.getServer();
        try {
            var registry = server.registryAccess().registryOrThrow(Registries.BIOME);
            Map<Integer, Holder<Biome>> map = new HashMap<>();
            for (BiomeGenBase b : LegacyBiomes.modBiomes()) {
                registry.getHolder(ResourceKey.create(Registries.BIOME, b.mcc$modernId())).ifPresent(h -> map.put(b.biomeID, h));
            }
            if (map.isEmpty()) return;
            LevelStem stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
            if (stem == null) return;
            WorldType type = worldType(stem);
            long seed = server.getWorldData().worldGenOptions().seed();
            mccompat.v1_7_10.net.minecraft.world.storage.WorldInfo.mcc$overworldType = type;
            BiomeOverride.set(new LegacyBiomeLayers(server, stem.generator().getBiomeSource(), type, seed, map));
            LOG.info("Placing {} legacy biome(s) in the overworld with the {} biome layers", map.size(), type.getWorldTypeName());
        } catch (RuntimeException | LinkageError e) {
            LOG.error("Legacy biome layers could not be set up; legacy biomes will not generate", e);
        }
    }

    /** Samples the overworld biome source on a coarse grid around spawn and logs how much legacy biome there is. */
    private static void logSample(MinecraftServer server) {
        var level = server.overworld();
        if (level == null) return;
        var generator = level.getChunkSource().getGenerator();
        var sampler = level.getChunkSource().randomState().sampler();
        Map<String, Integer> counts = new java.util.TreeMap<>();
        int total = 0;
        int legacy = 0;
        var spawn = level.getSharedSpawnPos();
        for (int dx = -64; dx <= 64; dx += 4) {
            for (int dz = -64; dz <= 64; dz += 4) {
                int qx = (spawn.getX() >> 2) + dx * 16;
                int qz = (spawn.getZ() >> 2) + dz * 16;
                Holder<Biome> b = generator.getBiomeSource().getNoiseBiome(qx, 16, qz, sampler);
                String id = b.unwrapKey().map(k -> k.location().toString()).orElse("?");
                total++;
                if (!id.startsWith("minecraft:")) {
                    legacy++;
                    counts.merge(id, 1, Integer::sum);
                }
            }
        }
        LOG.info("Biome sample around spawn: {} of {} points are legacy biomes {}", legacy, total, counts);
    }

    /**
     * The legacy world type of the world: a mod's type when the world was created with its preset (recognised by the
     * preset's own noise settings), else the default type, whose layers place biomes mods added to Forge's BiomeManager.
     */
    private static WorldType worldType(LevelStem stem) {
        if (stem.generator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise) {
            WorldType mod = noise.generatorSettings().unwrapKey().map(k -> LegacyWorldPresets.bySettings(k.location())).orElse(null);
            if (mod != null) return mod;
        }
        return WorldType.DEFAULT;
    }

    @Override
    public Holder<Biome> apply(BiomeSource source, Holder<Biome> modern, int x, int y, int z) {
        if (source != overworld || modern == null) return null;
        if (modern.is(BiomeTags.IS_OCEAN) || modern.is(BiomeTags.IS_DEEP_OCEAN) || modern.is(BiomeTags.IS_RIVER)
                || modern.is(BiomeTags.IS_BEACH) || modern.is(BiomeTags.IS_MOUNTAIN)) return null;
        if (modern.unwrapKey().map(KEEP::contains).orElse(false)) return null;
        return biomes.get(legacyAt(x, z));
    }

    /** Legacy biome id at a quart position (the legacy 1:4 biome map has the same scale). */
    private int legacyAt(int x, int z) {
        int rx = Math.floorDiv(x, REGION);
        int rz = Math.floorDiv(z, REGION);
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        int[] region = regions.get(key);
        if (region == null) {
            GenLayer l = layer();
            synchronized (l) { // legacy layers keep per-call state and are not thread-safe
                region = regions.computeIfAbsent(key, k -> l.getInts(rx * REGION, rz * REGION, REGION, REGION));
            }
            if (regions.size() > 4096) regions.clear();
        }
        return region[Math.floorMod(x, REGION) + Math.floorMod(z, REGION) * REGION];
    }
}
