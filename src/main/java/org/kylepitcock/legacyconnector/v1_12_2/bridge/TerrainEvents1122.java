package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.OreGenEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.PopulateChunkEvent;

/**
 * The terrain events 1.12.2 posted while it populated a chunk, before the mods' world generators ran: populate Pre, the biome
 * decorator's decorate Pre, ore Pre and Post (around vanilla's ores), decorate Post, populate Post. The modern generator makes the
 * vanilla features, so only the events go out; mods generate from them (ores on ore Post, structures on populate Post). Nothing is
 * built when no handler listens.
 */
public final class TerrainEvents1122 {
    /** Handlers keep a chunk's state in plain fields between the events: one chunk at a time. */
    private static final Object LOCK = new Object();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/WorldGen");
    private static final java.util.concurrent.atomic.AtomicInteger FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    private TerrainEvents1122() {}

    public static boolean wanted() {
        var bus = MinecraftForge.EVENT_BUS;
        var ores = MinecraftForge.ORE_GEN_BUS;
        return bus.mcc$hasListeners(PopulateChunkEvent.Pre.class) || bus.mcc$hasListeners(PopulateChunkEvent.Post.class)
                || bus.mcc$hasListeners(PopulateChunkEvent.class) || bus.mcc$hasListeners(DecorateBiomeEvent.Pre.class)
                || bus.mcc$hasListeners(DecorateBiomeEvent.Post.class) || bus.mcc$hasListeners(DecorateBiomeEvent.class)
                || ores.mcc$hasListeners(OreGenEvent.Pre.class) || ores.mcc$hasListeners(OreGenEvent.Post.class) || ores.mcc$hasListeners(OreGenEvent.class);
    }

    /** Posts the population events of a chunk, with vanilla's populate seed. A failing handler is logged (throttled). */
    public static void populate(World world, long seed, int chunkX, int chunkZ, IChunkGenerator generator) {
        if (!wanted()) return;
        Random rand = new Random(seed);
        long a = rand.nextLong() / 2L * 2L + 1L;
        long b = rand.nextLong() / 2L * 2L + 1L;
        rand.setSeed(chunkX * a + chunkZ * b ^ seed);
        BlockPos corner = new BlockPos(chunkX << 4, 0, chunkZ << 4);
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        synchronized (LOCK) {
            try {
                MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(generator, world, rand, chunkX, chunkZ, false));
                MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(world, rand, chunk));
                MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Pre(world, rand, corner));
                MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Post(world, rand, corner));
                MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(world, rand, chunk));
                MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(generator, world, rand, chunkX, chunkZ, false));
            } catch (RuntimeException | LinkageError e) {
                int n = FAILURES.incrementAndGet();
                if (n <= 3 || n % 500 == 0) LOG.error("A 1.12.2 terrain event handler failed in chunk {},{} ({} failures so far)", chunkX, chunkZ, n, e);
            }
        }
    }
}
