package legacyconnector.v1_7_10.net.minecraft.world.chunk.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;

/**
 * The chunk loader of a dimension folder ({@code region/r.X.Z.mca}). Saving is the modern game's: this loader reads. Whether a
 * chunk was saved comes from the region file's header (the Anvil layout is unchanged); a saved chunk of the world the folder
 * belongs to is loaded through the modern chunk cache (on the server thread), and other folders' chunks are not read.
 */
public class AnvilChunkLoader implements IChunkLoader, legacyconnector.v1_7_10.net.minecraft.world.storage.IThreadedFileIO {
    public final File chunkSaveLocation;

    public AnvilChunkLoader(File directory) {
        this.chunkSaveLocation = directory;
    }

    /** Whether the chunk is loaded in the world or saved in this folder's region file. */
    public boolean chunkExists(World world, int x, int z) {
        if (world != null && ownLevel(world) != null && world.chunkExists(x, z)) return true;
        return savedInRegion(chunkSaveLocation, x, z);
    }

    /** Whether the region file holds the chunk: its entry in the location table at the start of the file is set. */
    public static boolean savedInRegion(File dimensionFolder, int x, int z) {
        if (dimensionFolder == null) return false;
        File region = new File(new File(dimensionFolder, "region"), "r." + (x >> 5) + "." + (z >> 5) + ".mca");
        if (!region.isFile() || region.length() < 8192) return false;
        try (RandomAccessFile file = new RandomAccessFile(region, "r")) {
            file.seek(4L * ((x & 31) + (z & 31) * 32));
            return file.readInt() != 0;
        } catch (IOException e) {
            return false;
        }
    }

    /** The world's server level when this loader's folder is that level's own folder. */
    private net.minecraft.server.level.ServerLevel ownLevel(World world) {
        if (!(world.mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        try {
            var root = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            File own = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(), root).toFile();
            return own.getCanonicalFile().equals(chunkSaveLocation.getCanonicalFile()) ? level : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** The chunk: the loaded one, else (server thread, the world's own folder) loaded from its save; null when it is not saved here. */
    @Override
    public Chunk loadChunk(World world, int x, int z) throws IOException {
        if (world == null) return null;
        Chunk loaded = world.mcc$chunkOrNull(x, z);
        if (loaded != null) return loaded;
        var level = ownLevel(world);
        if (level == null || !level.getServer().isSameThread() || !savedInRegion(chunkSaveLocation, x, z)) return null;
        return new Chunk(world, level.getChunk(x, z));
    }

    /** Forge's asynchronous load: the chunk and its saved data (the data stays with the modern save). */
    public Object[] loadChunk__Async(World world, int x, int z) throws IOException {
        Chunk chunk = loadChunk(world, x, z);
        return chunk == null ? null : new Object[] {chunk, new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound()};
    }

    /** Chunks are saved by the modern game. */
    @Override
    public void saveChunk(World world, Chunk chunk) {
    }

    @Override
    public void saveExtraChunkData(World world, Chunk chunk) {
    }

    @Override
    public void chunkTick() {
    }

    @Override
    public void saveExtraData() {
    }

    /** Nothing is ever queued for writing. */
    @Override
    public boolean writeNextIO() {
        return false;
    }
}
