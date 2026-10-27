package legacyconnector.v1_12_2.net.minecraft.client.multiplayer;

import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;

/**
 * The client world's chunks ({@code WorldClient.getChunkProvider()}): the chunks the server sent. Chunks come and go with the server's
 * packets, so loading and unloading by hand changes nothing.
 */
public class ChunkProviderClient implements legacyconnector.v1_12_2.net.minecraft.world.chunk.IChunkProvider {
    private final legacyconnector.v1_12_2.net.minecraft.world.World world;

    public ChunkProviderClient(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        this.world = world;
    }

    @Override
    public Chunk getLoadedChunk(int x, int z) {
        return world.mcc$serverChunkProvider().getLoadedChunk(x, z);
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        return world.mcc$serverChunkProvider().provideChunk(x, z);
    }

    /** The chunk if the server sent it, else an empty one (as 1.12's client did). */
    public Chunk loadChunk(int x, int z) {
        return provideChunk(x, z);
    }

    public void unloadChunk(int x, int z) {
    }

    @Override
    public boolean tick() {
        return false;
    }

    @Override
    public String makeString() {
        return "MultiplayerChunkCache: " + world.mcc$level().getChunkSource().getLoadedChunksCount();
    }

    @Override
    public boolean isChunkGeneratedAt(int x, int z) {
        return getLoadedChunk(x, z) != null;
    }
}
