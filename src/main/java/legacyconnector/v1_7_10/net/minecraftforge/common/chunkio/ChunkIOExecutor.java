package legacyconnector.v1_7_10.net.minecraftforge.common.chunkio;

import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.AnvilChunkLoader;
import legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderServer;

/**
 * Forge's chunk loading off the server thread. The modern chunk system does the I/O itself, so a synchronous load is the
 * provider's own load (from the save or generated); an asynchronous one loads and then runs the callback.
 */
public class ChunkIOExecutor {
    public static Chunk syncChunkLoad(World world, AnvilChunkLoader loader, ChunkProviderServer provider, int x, int z) {
        return provider == null ? world.getChunkFromChunkCoords(x, z) : provider.originalLoadChunk(x, z);
    }

    public static void queueChunkLoad(World world, AnvilChunkLoader loader, ChunkProviderServer provider, int x, int z, Runnable runnable) {
        syncChunkLoad(world, loader, provider, x, z);
        if (runnable != null) runnable.run();
    }

    /** A queued load that is no longer wanted; loads here finish at once, so there is nothing to drop. */
    public static void dropQueuedChunkLoad(World world, int x, int z, Runnable runnable) {
    }

    public static void adjustPoolSize(int players) {
    }

    public static void tick() {
    }
}
