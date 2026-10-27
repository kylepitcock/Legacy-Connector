package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraft.world.level.WorldGenLevel;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldGenFeature;

/**
 * Runs the 1.12.2 {@link IWorldGenerator}s (ores, plants, small structures of mods) once per chunk, from the legacy world-gen
 * feature every biome carries, with Forge's per-chunk seeding so generation is reproducible. The world they get is a view of the
 * generation region (its own chunk and the neighbours 1.12 population spilled into). A failing generator is logged (throttled) and
 * the others still run. Legacy y=0..255 maps to the same modern heights; the deep layer below y=0 gets nothing.
 */
public final class WorldGenBridge1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/WorldGen");
    private static final AtomicInteger FAILURES = new AtomicInteger();
    private static final java.util.Map<IWorldGenerator, java.util.concurrent.locks.ReentrantLock> LOCKS =
            java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());
    private static boolean hooked;

    private WorldGenBridge1122() {}

    public static synchronized void hook() {
        if (hooked) return;
        hooked = true;
        LegacyWorldGenFeature.addDecorator(WorldGenBridge1122::decorate);
    }

    private static void decorate(WorldGenLevel level, int chunkX, int chunkZ, long worldSeed) {
        List<IWorldGenerator> gens = legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.GameRegistry.mcc$worldGenerators();
        boolean events = TerrainEvents1122.wanted();
        if (gens.isEmpty() && !events) return;
        World world = WorldBridge1122.wrap(level);
        try {
            // 1.12 populated the chunk (with its terrain events) before GameRegistry ran the mods' generators
            if (events) TerrainEvents1122.populate(world, worldSeed, chunkX, chunkZ, DUMMY);
            Random fmlRandom = new Random(worldSeed);
            long xSeed = fmlRandom.nextLong() >> 2 + 1L;
            long zSeed = fmlRandom.nextLong() >> 2 + 1L;
            long chunkSeed = (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
            var provider = world.getChunkProvider();
            for (IWorldGenerator g : gens) {
                fmlRandom.setSeed(chunkSeed);
                var lock = LOCKS.computeIfAbsent(g, org.kylepitcock.legacyconnector.core.LegacyModLocks::of);
                try {
                    // 1.12 decorated one chunk at a time on the server thread; a mod's generators keep state in plain fields
                    org.kylepitcock.legacyconnector.core.LegacyModLocks.generation(lock, () -> g.generate(fmlRandom, chunkX, chunkZ, world, DUMMY, provider));
                } catch (RuntimeException | LinkageError e) {
                    int n = FAILURES.incrementAndGet();
                    if (n <= 3 || n % 500 == 0) {
                        LOG.error("1.12.2 world generator {} failed in chunk {},{} ({} failures so far)", g.getClass().getName(), chunkX, chunkZ, n, e);
                    }
                }
            }
        } finally {
            WorldBridge1122.mcc$releaseRegion();
        }
    }

    /** The chunk generator legacy generators are handed: the modern game generates, so it offers nothing of its own. */
    private static final IChunkGenerator DUMMY = new IChunkGenerator() {
        @Override
        public Chunk generateChunk(int x, int z) {
            return null;
        }

        @Override
        public void populate(int x, int z) {
        }

        @Override
        public boolean generateStructures(Chunk chunkIn, int x, int z) {
            return false;
        }

        @Override
        public List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
            return new java.util.ArrayList<>();
        }

        @Override
        public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
            return null;
        }

        @Override
        public void recreateStructures(Chunk chunkIn, int x, int z) {
        }

        @Override
        public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
            return false;
        }
    };
}
