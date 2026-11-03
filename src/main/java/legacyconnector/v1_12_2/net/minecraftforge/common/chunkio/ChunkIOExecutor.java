package legacyconnector.v1_12_2.net.minecraftforge.common.chunkio;

import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.AnvilChunkLoader;
import legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkProviderServer;

/**
 * Forge's off-thread chunk loader. Loading a chunk and waiting for it from a tick handler never completes on the modern server (the
 * loads run on the server thread), so {@link #syncChunkLoad} answers what the chunk provider does: the loaded chunk, else an empty view.
 */
public class ChunkIOExecutor {
    public static Chunk syncChunkLoad(World world, AnvilChunkLoader loader, ChunkProviderServer provider, int x, int z) {
        return provider == null ? null : provider.provideChunk(x, z);
    }

    public static void queueChunkLoad(World world, AnvilChunkLoader loader, ChunkProviderServer provider, int x, int z, Runnable runnable) {
        if (runnable != null) runnable.run();
    }

    public static void dropQueuedChunkLoad(World world, int x, int z, Runnable runnable) {
    }

    public static void adjustPoolSize(int players) {
    }

    public static void tick() {
    }
}
