package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraft.world.level.WorldGenLevel;

/**
 * The terrain events 1.7.10 posted while it populated a chunk of the overworld, the Nether and the End: populate Pre, the
 * biome decorator's decorate Pre, ore Pre and Post (around vanilla's ores), decorate Post, populate Post. The modern
 * generator makes the vanilla features, so only the events are posted; mods generate from them (BuildCraft's oil and
 * NetherOres from populate, Railcraft's and Fisk's ores from ore Post). Chunks of legacy biomes get the decorate and ore
 * events from their own legacy decorator instead, and mod dimensions from their own chunk provider.
 */
final class TerrainEvents1710 {
    /** Handlers keep a chunk's state in plain fields between the events (Fisk's random): one chunk at a time. */
    private static final Object LOCK = new Object();

    private TerrainEvents1710() {}

    static void populate(WorldGenLevel level, World world, int chunkX, int chunkZ, IChunkProvider provider) {
        if (!wanted()) return;
        long seed = level.getSeed();
        Random rand = new Random(seed);
        long a = rand.nextLong() / 2L * 2L + 1L;
        long b = rand.nextLong() / 2L * 2L + 1L;
        rand.setSeed(chunkX * a + chunkZ * b ^ seed);
        boolean decorate = !LegacyBiomes.isLegacyChunk(level, chunkX, chunkZ);
        int x = chunkX << 4, z = chunkZ << 4;
        synchronized (LOCK) {
            MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(provider, world, rand, chunkX, chunkZ, false));
            if (decorate) {
                MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(world, rand, x, z));
                MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Pre(world, rand, x, z));
                MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Post(world, rand, x, z));
                MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(world, rand, x, z));
            }
            MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(provider, world, rand, chunkX, chunkZ, false));
        }
    }

    private static boolean wanted() {
        return MinecraftForge.EVENT_BUS.mcc$hasListeners(PopulateChunkEvent.Pre.class) || MinecraftForge.EVENT_BUS.mcc$hasListeners(PopulateChunkEvent.Post.class)
                || MinecraftForge.EVENT_BUS.mcc$hasListeners(DecorateBiomeEvent.Pre.class) || MinecraftForge.EVENT_BUS.mcc$hasListeners(DecorateBiomeEvent.Post.class)
                || MinecraftForge.ORE_GEN_BUS.mcc$hasListeners(OreGenEvent.Pre.class) || MinecraftForge.ORE_GEN_BUS.mcc$hasListeners(OreGenEvent.Post.class)
                || MinecraftForge.EVENT_BUS.mcc$hasListeners(PopulateChunkEvent.class) || MinecraftForge.ORE_GEN_BUS.mcc$hasListeners(OreGenEvent.class)
                || MinecraftForge.EVENT_BUS.mcc$hasListeners(DecorateBiomeEvent.class);
    }
}
