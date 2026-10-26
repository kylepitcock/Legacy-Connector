package legacyconnector.v1_7_10.net.minecraft.world.chunk.storage;

import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;

/** Loads and saves a dimension's chunks (1.7.10: the Anvil region files). */
public interface IChunkLoader {
    Chunk loadChunk(World world, int x, int z) throws java.io.IOException;

    void saveChunk(World world, Chunk chunk) throws legacyconnector.v1_7_10.net.minecraft.world.MinecraftException, java.io.IOException;

    void saveExtraChunkData(World world, Chunk chunk);

    void chunkTick();

    void saveExtraData();
}
