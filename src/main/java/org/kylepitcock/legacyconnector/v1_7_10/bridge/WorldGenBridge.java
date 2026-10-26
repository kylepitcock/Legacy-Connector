package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.cpw.mods.fml.common.IWorldGenerator;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.level.WorldGenLevel;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldGenFeature;

/** Runs 1.7.10 IWorldGenerators with the exact per-chunk seeding FML used, so generation is reproducible. */
public final class WorldGenBridge {
    private record Entry(IWorldGenerator generator, int weight, java.util.concurrent.locks.ReentrantLock lock) {}

    private static final List<Entry> GENERATORS = new ArrayList<>();
    private static boolean hooked;

    private WorldGenBridge() {}

    public static synchronized void register(IWorldGenerator generator, int weight) {
        GENERATORS.add(new Entry(generator, weight, org.kylepitcock.legacyconnector.core.LegacyModLocks.of(generator)));
        GENERATORS.sort(Comparator.comparingInt(Entry::weight));
        if (!hooked) {
            hooked = true;
            LegacyWorldGenFeature.addDecorator(WorldGenBridge::decorate);
        }
    }

    private static void decorate(WorldGenLevel level, int chunkX, int chunkZ, long worldSeed) {
        decorate(level, chunkX, chunkZ, worldSeed, DUMMY);
    }

    /**
     * Runs the generators in a chunk of a mod dimension. 1.7.10 ran them from {@code Chunk.populateChunk} right after the
     * dimension's own {@code IChunkProvider.populate}, in every dimension (generators check the dimension id themselves:
     * Thaumcraft builds the Outer Lands maze this way); the modern biome feature that runs them elsewhere is not part of a
     * mod dimension's decoration, so its chunk generator calls this. A failing generator is logged (throttled) and skipped.
     */
    public static void decorateModDimension(WorldGenLevel level, int chunkX, int chunkZ, IChunkProvider chunkGenerator) {
        decorate(level, chunkX, chunkZ, level.getSeed(), chunkGenerator == null ? DUMMY : chunkGenerator);
    }

    private static void decorate(WorldGenLevel level, int chunkX, int chunkZ, long worldSeed, IChunkProvider chunkGenerator) {
        List<Entry> gens;
        synchronized (WorldGenBridge.class) {
            gens = List.copyOf(GENERATORS);
        }
        World world = World.mcc$wrap(level);
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        long chunkSeed = (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
        // 1.7.10 decorated one chunk at a time on the server thread, and generators keep their state in plain fields and
        // HashMaps (CustomOreGen's per-world caches and geometry buffers): a mod's generators run on one thread at a time.
        for (Entry e : gens) {
            fmlRandom.setSeed(chunkSeed);
            if (chunkGenerator == DUMMY) {
                org.kylepitcock.legacyconnector.core.LegacyModLocks.generation(e.lock, () -> e.generator.generate(fmlRandom, chunkX, chunkZ, world, DUMMY, DUMMY));
                continue;
            }
            try {
                org.kylepitcock.legacyconnector.core.LegacyModLocks.generation(e.lock, () -> e.generator.generate(fmlRandom, chunkX, chunkZ, world, chunkGenerator, DUMMY));
            } catch (RuntimeException | LinkageError ex) {
                int n = DIMENSION_FAILURES.incrementAndGet();
                if (n <= 3 || n % 200 == 0) {
                    LOG.error("Legacy world generator {} failed in chunk {},{} of a mod dimension ({} failures so far)", e.generator.getClass().getName(),
                            chunkX, chunkZ, n, ex);
                }
            }
        }
        // 1.7.10 worlds ended at y=0; below that is the deep layer (deepslate, bedrock at -64). Generators run once more
        // with the world shifted so that legacy y=0 is the modern bottom: what spawned deep or in bedrock spawns there too.
        int bottom = level.getMinBuildHeight();
        if (bottom >= 0 || !org.kylepitcock.legacyconnector.core.LegacyConfig.deepLayerEnabled()) return;
        World.mcc$enterDeepPass(bottom);
        try {
            for (Entry e : gens) {
                if (!org.kylepitcock.legacyconnector.core.LegacyConfig.deepLayerAllows(e.generator.getClass().getName())) continue;
                fmlRandom.setSeed(chunkSeed ^ DEEP_SEED);
                try {
                    org.kylepitcock.legacyconnector.core.LegacyModLocks.generation(e.lock, () -> e.generator.generate(fmlRandom, chunkX, chunkZ, world, DUMMY, DUMMY));
                } catch (RuntimeException | LinkageError ex) {
                    if (DEEP_FAILED.add(e.generator.getClass().getName())) {
                        LOG.warn("Legacy world generator {} failed in the deep layer (it keeps running above y=0): {}",
                                e.generator.getClass().getName(), ex.toString());
                    }
                }
            }
        } finally {
            World.mcc$leaveDeepPass();
        }
    }

    private static final long DEEP_SEED = 0x5DEECE66DL;
    private static final java.util.concurrent.atomic.AtomicInteger DIMENSION_FAILURES = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.Set<String> DEEP_FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/WorldGen");

    private static final IChunkProvider DUMMY = new IChunkProvider() {
        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }

        @Override
        public String makeString() {
            return "LegacyConnectorChunkProvider";
        }

        @Override
        public int getLoadedChunkCount() {
            return 0;
        }
    };

    static List<IWorldGenerator> generators() {
        List<IWorldGenerator> out = new ArrayList<>();
        for (Entry e : GENERATORS) out.add(e.generator);
        return out;
    }
}
