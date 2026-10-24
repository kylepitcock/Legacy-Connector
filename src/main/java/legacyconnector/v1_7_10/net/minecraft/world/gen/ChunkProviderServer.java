package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

/**
 * The chunk provider of a server world, what {@code world.getChunkProvider()} returns. Mods cast it
 * ({@code (ChunkProviderServer) world.getChunkProvider()}) to read the loaded chunks; the chunks themselves are managed by
 * the modern chunk cache, so the lists here are empty and the operations answer through the world.
 */
public class ChunkProviderServer implements IChunkProvider {
    public final World mcc$world;
    public Set<Long> chunksToUnload = new HashSet<>();
    public boolean loadChunkOnProvideRequest = true;
    public IChunkProvider currentChunkProvider;

    /** Phantom: redirected to {@link #mcc$get$loadedChunks}. */
    @Deprecated
    public List loadedChunks;

    public ChunkProviderServer(World world) {
        this.mcc$world = world;
    }

    public static List mcc$get$loadedChunks(ChunkProviderServer p) {
        return new ArrayList();
    }

    public List func_152380_a() {
        return mcc$get$loadedChunks(this);
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return mcc$world.chunkExists(x, z);
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        return mcc$world.getChunkFromChunkCoords(x, z);
    }

    @Override
    public Chunk loadChunk(int x, int z) {
        return provideChunk(x, z);
    }

    @Override
    public String makeString() {
        return "ServerChunkCache";
    }

    @Override
    public int getLoadedChunkCount() {
        return mcc$world.mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel s ? s.getChunkSource().getLoadedChunksCount() : 0;
    }

    /** The modern chunk cache unloads chunks itself. */
    public void unloadChunksIfNotNearSpawn(int x, int z) {
    }

    public void unloadAllChunks() {
    }
}
