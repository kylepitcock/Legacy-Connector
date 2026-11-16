package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface IChunkLoadHandler {
    void addActiveChunks(legacyconnector.v1_7_10.net.minecraft.world.World world, java.util.Set<legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair> chunks);

    boolean canUnloadChunk(legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk chunk);

    boolean canUpdateEntity(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity);
}
