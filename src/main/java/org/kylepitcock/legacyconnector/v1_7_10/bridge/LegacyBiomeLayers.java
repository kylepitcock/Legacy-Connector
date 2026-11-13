package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.kylepitcock.legacyconnector.core.worldgen.BiomePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy mod biomes in the overworld.
 * <ul>
 * <li>Default world: the biomes enter the overworld through the connector-owned multi-noise overlay ({@link BiomePlacement}).
 * TerraBlender is used only as an optional integration when already installed. Climate and weight come from the legacy data
 * ({@link LegacyBiomeClimate}).</li>
 * <li>World created with a legacy mod's world type preset (BoP's "BIOMESOP"): its biome source is a {@link LegacyLayeredBiomeSource},
 * whose land biomes come from that world type's 1.7.10 biome layers (this class holds the layer state of one such source).</li>
 * </ul>
 */
public final class LegacyBiomeLayers {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Biomes");
    private static final int REGION = 32;
    /** Modern biomes tied to their terrain (caves, peaks and slopes, shores): never replaced. */
    private static final Set<ResourceKey<Biome>> KEEP = Set.of(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK,
            Biomes.MUSHROOM_FIELDS, Biomes.STONY_SHORE, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS,
            Biomes.SNOWY_SLOPES);
    private static boolean registered;
    /** Placement mode of the running server, for the spawn sample log. */
    private static volatile String mode = "none";
    private static volatile LegacyLayeredBiomeSource bound;

    private final MinecraftServer server;
    private final WorldType type;
    private final long seed;
    private volatile GenLayer layer;
    private final Map<Integer, Holder<Biome>> biomes;
    private final Map<Long, int[]> regions = new ConcurrentHashMap<>();

    private LegacyBiomeLayers(MinecraftServer server, WorldType type, long seed, Map<Integer, Holder<Biome>> biomes) {
        this.server = server;
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
                    var manager = type.getChunkManager(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level));
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
        BiomePlacement.setEntries(LegacyBiomeClimate::entries);
        NeoForge.EVENT_BUS.addListener( LegacyBiomeLayers::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> unbind());
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            // a diagnostic (1089 biome source lookups): off the server thread, which the joining player waits for (perf.asyncBiomeSample)
            if (org.kylepitcock.legacyconnector.core.LegacyConfig.perfAsyncBiomeSample()) net.minecraft.Util.backgroundExecutor().execute(() -> logSample(e.getServer()));
            else logSample(e.getServer());
        });
    }

    private static void unbind() {
        LegacyLayeredBiomeSource s = bound;
        bound = null;
        if (s != null) s.mcc$bind(null);
        mode = "none";
        BiomePlacement.clearNative();
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        unbind();
        MinecraftServer server = event.getServer();
        legacyconnector.v1_7_10.net.minecraft.world.storage.WorldInfo.mcc$overworldType = WorldType.DEFAULT;
        try {
            mode = "none";
            LevelStem stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
            if (stem != null) {
                var source = stem.generator().getBiomeSource();
                if (source instanceof LegacyLayeredBiomeSource layered) {
                    bindLayers(server, layered);
                } else if (source instanceof MultiNoiseBiomeSource multiNoise) {
                    if (BiomePlacement.hasTerraBlenderRegion()) {
                        mode = "TerraBlender region";
                    } else if (BiomePlacement.bindNative(server, multiNoise)) {
                        mode = "native multi-noise overlay";
                    } else {
                        mode = "original multi-noise source";
                    }
                } else {
                    mode = "custom biome source (native overlay not applicable)";
                    warnOldLayeredWorld(stem);
                    LOG.warn("Overworld biome source {} is not multi-noise; the connector leaves it unchanged and legacy default-world biome placement is unavailable",
                            source.getClass().getName());
                }
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.error("Legacy biome placement could not be set up; legacy biomes may not generate", e);
        }
    }

    /** A world of a legacy world type created before its preset had its own biome source: it gets the default placement. */
    private static void warnOldLayeredWorld(LevelStem stem) {
        if (!(stem.generator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise)) return;
        WorldType mod = noise.generatorSettings().unwrapKey().map(k -> LegacyWorldPresets.bySettings(k.location())).orElse(null);
        if (mod != null) {
            LOG.info("This world was created with world type {} before it had its own biome source; new chunks place legacy biomes by climate",
                    mod.getWorldTypeName());
        }
    }

    private static void bindLayers(MinecraftServer server, LegacyLayeredBiomeSource source) {
        WorldType type = null;
        for (WorldType t : LegacyWorldPresets.modTypes()) if (t.getWorldTypeName().equals(source.mcc$worldType())) type = t;
        if (type == null) {
            LOG.warn("World type {} of this world is not installed; the overworld uses the multi-noise biomes", source.mcc$worldType());
            return;
        }
        var registry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Map<Integer, Holder<Biome>> map = new HashMap<>();
        int modCount = 0;
        for (BiomeGenBase b : LegacyBiomes.modBiomes()) {
            var holder = registry.getHolder(ResourceKey.create(Registries.BIOME, b.mcc$modernId()));
            if (holder.isPresent()) {
                map.put(b.biomeID, holder.get());
                modCount++;
            }
        }
        long seed = server.getWorldData().worldGenOptions().seed();
        legacyconnector.v1_7_10.net.minecraft.world.storage.WorldInfo.mcc$overworldType = type;
        source.mcc$bind(new LegacyBiomeLayers(server, type, seed, map));
        bound = source;
        mode = type.getWorldTypeName() + " biome layers";
        LOG.info("World type {}: its biome layers choose the overworld land biomes ({} legacy biome(s))", type.getWorldTypeName(), modCount);
    }

    /** Samples the overworld biome source on a coarse grid around spawn and logs how much legacy biome there is. */
    private static void logSample(MinecraftServer server) {
        try {
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
                    if (b.unwrapKey().map(k -> LegacyBiomes.byModernId(k.location()) != null).orElse(false)) {
                        legacy++;
                        counts.merge(id, 1, Integer::sum);
                    }
                }
            }
            LOG.info("Biome sample around spawn ({}): {} of {} points are legacy biomes {}", mode, legacy, total, counts);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Biome sample around spawn failed: {}", e.toString());
        }
    }

    /** The legacy layout's mod biome at quart position (x, z) where it may replace {@code modern}, else null. */
    Holder<Biome> replace(Holder<Biome> modern, int x, int z) {
        if (modern.is(BiomeTags.IS_OCEAN) || modern.is(BiomeTags.IS_DEEP_OCEAN) || modern.is(BiomeTags.IS_RIVER)
                || modern.is(BiomeTags.IS_BEACH)) return null;
        if (modern.unwrapKey().map(KEEP::contains).orElse(false)) return null;
        // Only mod biomes replace what the modern source chose: where the legacy layout has a vanilla biome, the modern one stays,
        // so modern biome mods (TerraBlender regions) keep theirs.
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
