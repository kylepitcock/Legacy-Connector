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

    /**
     * FML's {@code registerWorldGenerator}: its generators are a set with a weight map, so registering a generator again only changes
     * its weight. Listing it twice ran it twice per chunk and put its placed feature twice into every biome's feature list, which is a
     * feature order cycle: vanilla's feature indexer fails (Biolith falls back to its slow resilient indexer for every dimension; in All
     * the Mods 10 with ChromatiCraft, which registers DataTowerGenerator twice, 858 of its 1412 dropped edges and most of the 18 s it
     * spent before the levels were created).
     */
    public static synchronized void register(IWorldGenerator generator, int weight) {
        for (int i = 0; i < GENERATORS.size(); i++) {
            Entry known = GENERATORS.get(i);
            if (known.generator != generator) continue;
            if (known.weight != weight) {
                GENERATORS.set(i, new Entry(generator, weight, known.lock, known.modId));
                GENERATORS.sort(Comparator.comparingInt(Entry::weight));
            }
            return;
        }
        GENERATORS.add(new Entry(generator, weight, org.kylepitcock.legacyconnector.core.LegacyModLocks.of(generator), modIdOf(generator)));
        DeepLayer1710.register(generator, weight, org.kylepitcock.legacyconnector.core.LegacyModLocks.of(generator)); // deep layer feature
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
     * Runs one generator for one chunk of an ordinary dimension, as {@link #decorate} would: FML's chunk seed and the mod's lock. Its
     * deep-layer pass is a placed feature of its own ({@link DeepLayer1710}). Throws what the generator throws (the feature logs it).
     */
    private static void generateOne(WorldGenLevel level, int chunkX, int chunkZ, Entry e) {
        World world = World.mcc$wrap(level);
        long chunkSeed = chunkSeed(level.getSeed(), chunkX, chunkZ);
        Random fmlRandom = new Random(chunkSeed);
        // ---- InPlaceStructureGuard: a call that builds into a modern structure is undone (see the class)
        World.mcc$setOreSplit(splitsOres(level, e));
        try {
            org.kylepitcock.legacyconnector.core.worldgen.InPlaceStructureGuard.run(level, e.generator,
                    () -> org.kylepitcock.legacyconnector.core.LegacyModLocks.generation(e.lock, () -> e.generator.generate(fmlRandom, chunkX, chunkZ, world, DUMMY, DUMMY)));
        } finally {
            World.mcc$setOreSplit(false);
        }
    }

    /** Whether the generator's deep layer runs here, so its ores in legacy y 0-63 are shared with it (World.mcc$splitDrops). */
    private static boolean splitsOres(WorldGenLevel level, Entry e) {
        return level.getMinBuildHeight() < 0 && org.kylepitcock.legacyconnector.core.LegacyConfig.deepLayerEnabled()
                && org.kylepitcock.legacyconnector.core.LegacyConfig.deepLayerAllows(e.generator.getClass().getName());
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
        if (!modDimension) org.kylepitcock.legacyconnector.core.worldgen.InPlaceStructureGuard.run(level, "populate events",
                () -> TerrainEvents1710.populate(level, world, chunkX, chunkZ, DUMMY));
        // 1.7.10 decorated one chunk at a time on the server thread, and generators keep their state in plain fields and
        // HashMaps (CustomOreGen's per-world caches and geometry buffers): a mod's generators run on one thread at a time.
        for (Entry e : gens) {
            fmlRandom.setSeed(chunkSeed);
            IChunkProvider provider = modDimension ? chunkGenerator : DUMMY;
            try {
                Runnable call = () -> org.kylepitcock.legacyconnector.core.LegacyModLocks.generation(e.lock, () -> e.generator.generate(fmlRandom, chunkX, chunkZ, world, provider, DUMMY));
                if (modDimension) call.run();
                else {
                    World.mcc$setOreSplit(splitsOres(level, e));
                    try {
                        org.kylepitcock.legacyconnector.core.worldgen.InPlaceStructureGuard.run(level, e.generator, call); // InPlaceStructureGuard
                    } finally {
                        World.mcc$setOreSplit(false);
                    }
                }
            } catch (RuntimeException | LinkageError ex) {
                int n = DIMENSION_FAILURES.incrementAndGet();
                if (n <= 3 || n % 200 == 0) {
                    LOG.error("Legacy world generator {} failed in chunk {},{}{} ({} failures so far)", e.generator.getClass().getName(),
                            chunkX, chunkZ, modDimension ? " of a mod dimension" : "", n, ex);
                }
            }
        }
        // ---- deep layer (DeepLayer1710): each generator's deep pass is a placed feature of its own; this runs the rest
        DeepLayer1710.runUncovered(level, world, chunkX, chunkZ, chunkSeed, modDimension);
    }

    private static final java.util.concurrent.atomic.AtomicInteger DIMENSION_FAILURES = new java.util.concurrent.atomic.AtomicInteger();
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

    /** The placeholder chunk provider legacy generators get (deep layer, DeepLayer1710). */
    static IChunkProvider dummyProvider() {
        return DUMMY;
    }

    static List<IWorldGenerator> generators() {
        List<IWorldGenerator> out = new ArrayList<>();
        for (Entry e : GENERATORS) out.add(e.generator);
        return out;
    }
}
