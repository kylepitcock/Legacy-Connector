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
    /** The loader of the dimension's saved chunks (its folder of region files). */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.IChunkLoader currentChunkLoader;

    /** Phantom: redirected to {@link #mcc$get$loadedChunks}. */
    @Deprecated
    public List loadedChunks;

    /**
     * Loaded chunks by {@code ChunkCoordIntPair.chunkXZ2Int} key (IC2's {@code Util.getLoadedChunk} reads it): answers from the
     * world without loading anything.
     */
    public legacyconnector.v1_7_10.net.minecraft.util.LongHashMap loadedChunkHashMap;

    public ChunkProviderServer(World world) {
        this.mcc$world = world;
        this.currentChunkLoader = mcc$loader(world);
        this.loadedChunkHashMap = new legacyconnector.v1_7_10.net.minecraft.util.LongHashMap() {
            @Override
            public Object getValueByKey(long key) {
                int x = (int) (key & 0xFFFFFFFFL);
                int z = (int) (key >>> 32);
                return mcc$world.chunkExists(x, z) ? mcc$world.getChunkFromChunkCoords(x, z) : null;
            }

            @Override
            public boolean containsItem(long key) {
                return getValueByKey(key) != null;
            }
        };
    }

    /** A chunk provider over the world with its own loader and generator (mods' dummy providers). */
    public ChunkProviderServer(legacyconnector.v1_7_10.net.minecraft.world.WorldServer world,
            legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.IChunkLoader loader, IChunkProvider generator) {
        this(world);
        this.currentChunkLoader = loader;
        this.currentChunkProvider = generator;
    }

    /** The dimension folder's loader of a server world (null elsewhere). */
    private static legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.IChunkLoader mcc$loader(World world) {
        if (world == null || !(world.mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        try {
            var root = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            return new legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.AnvilChunkLoader(
                    net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(), root).toFile());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Forge: loads the chunk now, from its save or by generating it (on the server thread; elsewhere only a loaded chunk is
     * returned, since a worker waiting for the main thread's chunk cache could deadlock).
     */
    public Chunk originalLoadChunk(int x, int z) {
        Chunk loaded = mcc$world.getChunkFromChunkCoords(x, z);
        if (loaded != null) return loaded;
        if (mcc$world.mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel level && level.getServer().isSameThread()) {
            return new Chunk(mcc$world, level.getChunk(x, z));
        }
        return null;
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
        Chunk chunk = mcc$world.getChunkFromChunkCoords(x, z);
        // 1.7.10's provideChunk never answered null: an unavailable chunk was the empty chunk (Mystcraft's generator dereferences it)
        return chunk != null ? chunk : new legacyconnector.v1_7_10.net.minecraft.world.chunk.EmptyChunk(mcc$world, x, z);
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
