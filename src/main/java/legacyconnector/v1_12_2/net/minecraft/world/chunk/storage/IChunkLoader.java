package legacyconnector.v1_12_2.net.minecraft.world.chunk.storage;

import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;

/** 1.12.2 chunk storage of a dimension. */
public interface IChunkLoader {
    Chunk loadChunk(World worldIn, int x, int z) throws java.io.IOException;

    void saveChunk(World worldIn, Chunk chunkIn) throws legacyconnector.v1_12_2.net.minecraft.world.MinecraftException, java.io.IOException;

    void saveExtraChunkData(World worldIn, Chunk chunkIn) throws java.io.IOException;

    void chunkTick();

    void flush();

    boolean isChunkGeneratedAt(int x, int z);
}
