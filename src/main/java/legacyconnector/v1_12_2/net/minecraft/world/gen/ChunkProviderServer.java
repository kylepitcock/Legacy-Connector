package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.Collection;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.IChunkProvider;

/**
 * The chunks of a world as 1.12.2's server chunk provider. Only chunks the modern game has loaded (or is generating here) are
 * provided without loading: legacy code asks for chunks from tile entity ticks and world generators, where loading would wait for
 * the server thread. {@link #chunkGenerator} is the dimension's legacy generator when it has one (null for modern dimensions).
 */
public class ChunkProviderServer implements IChunkProvider {
    private final World world;
    public IChunkGenerator chunkGenerator;

    public ChunkProviderServer(World world) {
        this.world = world;
    }

    private net.minecraft.world.level.LevelAccessor access() {
        return world.mcc$access();
    }

    @Override
    public Chunk getLoadedChunk(int x, int z) {
        var access = access();
        if (access == null) return null;
        if (access instanceof net.minecraft.server.level.ServerLevel sl) {
            var chunk = sl.getChunkSource().getChunkNow(x, z);
            return chunk == null ? null : Chunk.mcc$of(world, chunk);
        }
        var chunk = access.getChunk(x, z, net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY, false);
        return chunk == null ? null : Chunk.mcc$of(world, chunk);
    }

    /** The chunk, loading it on the server thread (elsewhere only a loaded one; an empty chunk view otherwise). */
    @Override
    public Chunk provideChunk(int x, int z) {
        Chunk loaded = getLoadedChunk(x, z);
        if (loaded != null) return loaded;
        var access = access();
        if (access instanceof net.minecraft.server.level.ServerLevel sl && sl.getServer().isSameThread()) {
            return Chunk.mcc$of(world, sl.getChunk(x, z));
        }
        return new Chunk(world, x, z);
    }

    public Chunk loadChunk(int x, int z) {
        return getLoadedChunk(x, z);
    }

    public Collection<Chunk> getLoadedChunks() {
        List<Chunk> out = new java.util.ArrayList<>();
        if (access() instanceof net.minecraft.server.level.ServerLevel sl) {
            for (var holder : sl.getChunkSource().chunkMap.getChunks()) {
                var chunk = holder.getTickingChunk();
                if (chunk != null) out.add(Chunk.mcc$of(world, chunk));
            }
        }
        return out;
    }

    public void queueUnload(Chunk chunk) {
    }

    public void queueUnloadAll() {
    }

    public boolean saveChunks(boolean all) {
        return true;
    }

    public void flushToDisk() {
    }

    @Override
    public boolean tick() {
        return false;
    }

    public boolean canSave() {
        return true;
    }

    @Override
    public String makeString() {
        return "ServerChunkCache: " + getLoadedChunkCount();
    }

    public List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        if (chunkGenerator != null) return chunkGenerator.getPossibleCreatures(type, pos);
        var biome = world.getBiome(pos);
        return biome == null ? new java.util.ArrayList<>() : biome.getSpawnableList(type);
    }

    public BlockPos getNearestStructurePos(World w, String structureName, BlockPos position, boolean findUnexplored) {
        if (chunkGenerator != null) return chunkGenerator.getNearestStructurePos(w, structureName, position, findUnexplored);
        return null;
    }

    public boolean isInsideStructure(World w, String structureName, BlockPos pos) {
        return chunkGenerator != null && chunkGenerator.isInsideStructure(w, structureName, pos);
    }

    public int getLoadedChunkCount() {
        return access() instanceof net.minecraft.server.level.ServerLevel sl ? sl.getChunkSource().getLoadedChunksCount() : 0;
    }

    public boolean chunkExists(int x, int z) {
        var access = access();
        if (access instanceof net.minecraft.server.level.ServerLevel sl) return sl.getChunkSource().getChunkNow(x, z) != null;
        return access != null && access.hasChunk(x, z);
    }

    @Override
    public boolean isChunkGeneratedAt(int x, int z) {
        if (chunkExists(x, z)) return true;
        if (access() instanceof net.minecraft.server.level.ServerLevel sl) {
            try {
                var tag = sl.getChunkSource().chunkMap.read(new net.minecraft.world.level.ChunkPos(x, z)).getNow(java.util.Optional.empty());
                return tag.isPresent();
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }
}
