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

/**
 * Runs 1.7.10 IWorldGenerators with the exact per-chunk seeding FML used, so generation is reproducible. In ordinary dimensions each
 * generator is a placed feature of its own ({@link org.kylepitcock.legacyconnector.core.worldgen.LegacyGenerators}, which calls
 * {@link #generateOne}); the catch-all feature posts the populate terrain events and runs only the generators without a feature of
 * their own. Mod dimensions with a legacy chunk provider run them all from {@link #decorateModDimension}.
 */
public final class WorldGenBridge {
    private record Entry(IWorldGenerator generator, int weight, java.util.concurrent.locks.ReentrantLock lock, String modId) {}

    private static final List<Entry> GENERATORS = new ArrayList<>();
    private static boolean hooked;

    private WorldGenBridge() {}

    public static synchronized void register(IWorldGenerator generator, int weight) {
        GENERATORS.add(new Entry(generator, weight, org.kylepitcock.legacyconnector.core.LegacyModLocks.of(generator), modIdOf(generator)));
        GENERATORS.sort(Comparator.comparingInt(Entry::weight));
        hook();
    }

    /** Runs per chunk from now on (also with no generators: the terrain events still go out). */
    static synchronized void hook() {
        if (!hooked) {
            hooked = true;
            LegacyWorldGenFeature.addDecorator(WorldGenBridge::decorate);
            org.kylepitcock.legacyconnector.core.worldgen.LegacyGenerators.addSource(WorldGenBridge::features);
        }
    }

    /** The generators in FML's order (weight, then registration: the sort is stable), each run as a feature of its own. */
    private static List<org.kylepitcock.legacyconnector.core.worldgen.LegacyGenerators.Generator> features() {
        List<org.kylepitcock.legacyconnector.core.worldgen.LegacyGenerators.Generator> out = new ArrayList<>();
        synchronized (WorldGenBridge.class) {
            for (Entry e : GENERATORS) {
                out.add(new org.kylepitcock.legacyconnector.core.worldgen.LegacyGenerators.Generator(e.generator, e.modId,
                        (level, chunkX, chunkZ) -> generateOne(level, chunkX, chunkZ, e)));
            }
        }
        return out;
    }

    /**
     * Runs one generator for one chunk of an ordinary dimension, as {@link #decorate} would: FML's chunk seed, the mod's lock, then its
     * deep-layer pass. Throws what the generator throws (the per-generator feature logs it).
     */
    private static void generateOne(WorldGenLevel level, int chunkX, int chunkZ, Entry e) {
        World world = World.mcc$wrap(level);
        long chunkSeed = chunkSeed(level.getSeed(), chunkX, chunkZ);
        Random fmlRandom = new Random(chunkSeed);
        org.kylepitcock.legacyconnector.core.LegacyModLocks.generation(e.lock, () -> e.generator.generate(fmlRandom, chunkX, chunkZ, world, DUMMY, DUMMY));
        deepPass(level, world, chunkX, chunkZ, chunkSeed, List.of(e));
    }

    /** FML's per-chunk seed ({@code GameRegistry.generateWorld}): every generator starts from it. */
    private static long chunkSeed(long worldSeed, int chunkX, int chunkZ) {
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        return (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
    }

    /** The legacy mod that registered a generator (the active mod), else the mod whose jar (module) the generator's class is in. */
    private static String modIdOf(IWorldGenerator generator) {
        try {
            var loader = legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.instance();
            var active = loader.activeModContainer();
            if (active != null && active.getModId() != null) return active.getModId();
            Class<?> type = generator.getClass();
            for (var c : loader.getActiveModList()) {
                Object mod = c.getMod();
                if (mod == null) continue;
                Class<?> modType = mod.getClass();
                if (type.getModule().isNamed() ? modType.getModule() == type.getModule() : modType.getClassLoader() == type.getClassLoader()) return c.getModId();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // no id: the feature gets the layer's namespace
        }
        return null;
    }

    private static void decorate(WorldGenLevel level, int chunkX, int chunkZ, long worldSeed) {
        decorate(level, chunkX, chunkZ, worldSeed, DUMMY, false);
    }

    /**
     * Runs the generators in a chunk of a mod dimension. 1.7.10 ran them from {@code Chunk.populateChunk} right after the
     * dimension's own {@code IChunkProvider.populate}, in every dimension (generators check the dimension id themselves:
     * Thaumcraft builds the Outer Lands maze this way); the modern biome feature that runs them elsewhere is not part of a
     * mod dimension's decoration, so its chunk generator calls this. A failing generator is logged (throttled) and skipped.
     */
    public static void decorateModDimension(WorldGenLevel level, int chunkX, int chunkZ, IChunkProvider chunkGenerator) {
        decorate(level, chunkX, chunkZ, level.getSeed(), chunkGenerator == null ? DUMMY : chunkGenerator, true);
    }

    private static void decorate(WorldGenLevel level, int chunkX, int chunkZ, long worldSeed, IChunkProvider chunkGenerator, boolean modDimension) {
        List<Entry> gens;
        synchronized (WorldGenBridge.class) {
            gens = List.copyOf(GENERATORS);
        }
        // in ordinary dimensions the generators with a placed feature of their own run from it (after this one, in legacy order)
        if (!modDimension) gens = gens.stream().filter(e -> !org.kylepitcock.legacyconnector.core.worldgen.LegacyGenerators.covered(level, e.generator)).toList();
        World world = World.mcc$wrap(level);
        Random fmlRandom = new Random(worldSeed);
        long chunkSeed = chunkSeed(worldSeed, chunkX, chunkZ);
        // the chunk provider's populate came first in 1.7.10, with its terrain events
        if (!modDimension) TerrainEvents1710.populate(level, world, chunkX, chunkZ, DUMMY);
        // 1.7.10 decorated one chunk at a time on the server thread, and generators keep their state in plain fields and
        // HashMaps (CustomOreGen's per-world caches and geometry buffers): a mod's generators run on one thread at a time.
        for (Entry e : gens) {
            fmlRandom.setSeed(chunkSeed);
            IChunkProvider provider = modDimension ? chunkGenerator : DUMMY;
            try {
                org.kylepitcock.legacyconnector.core.LegacyModLocks.generation(e.lock, () -> e.generator.generate(fmlRandom, chunkX, chunkZ, world, provider, DUMMY));
            } catch (RuntimeException | LinkageError ex) {
                int n = DIMENSION_FAILURES.incrementAndGet();
                if (n <= 3 || n % 200 == 0) {
                    LOG.error("Legacy world generator {} failed in chunk {},{}{} ({} failures so far)", e.generator.getClass().getName(),
                            chunkX, chunkZ, modDimension ? " of a mod dimension" : "", n, ex);
                }
            }
        }
        deepPass(level, world, chunkX, chunkZ, chunkSeed, gens);
    }

    /**
     * 1.7.10 worlds ended at y=0; below that is the deep layer (deepslate, bedrock at -64). Generators run once more with the world
     * shifted so that legacy y=0 is the modern bottom: what spawned deep or in bedrock spawns there too. A failing generator is logged
     * once and skipped.
     */
    private static void deepPass(WorldGenLevel level, World world, int chunkX, int chunkZ, long chunkSeed, List<Entry> gens) {
        if (gens.isEmpty()) return;
        int bottom = level.getMinBuildHeight();
        if (bottom >= 0 || !org.kylepitcock.legacyconnector.core.LegacyConfig.deepLayerEnabled()) return;
        Random fmlRandom = new Random();
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
