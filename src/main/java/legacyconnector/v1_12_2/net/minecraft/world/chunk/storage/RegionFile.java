package legacyconnector.v1_12_2.net.minecraft.world.chunk.storage;

import java.io.DataInputStream;
import java.io.File;

/**
 * 1.12.2 region file (.mca, 32x32 chunks): reads through the modern region file of the same format. The chunk data it returns is
 * the modern chunk NBT. Writing is left to the game (a second writer would corrupt the file the server keeps open).
 */
public class RegionFile {
    private final File fileName;
    private net.minecraft.world.level.chunk.storage.RegionFile mcc$modern;

    public RegionFile(File fileNameIn) {
        this.fileName = fileNameIn;
        try {
            var info = new net.minecraft.world.level.chunk.storage.RegionStorageInfo("legacy", net.minecraft.world.level.Level.OVERWORLD, "chunk");
            this.mcc$modern = new net.minecraft.world.level.chunk.storage.RegionFile(info, fileNameIn.toPath(), fileNameIn.getParentFile().toPath(), false);
        } catch (java.io.IOException | RuntimeException e) {
            this.mcc$modern = null;
        }
    }

    /** Local chunk coordinates (0..31) inside the region. */
    public synchronized DataInputStream getChunkDataInputStream(int x, int z) {
        if (mcc$modern == null || x < 0 || x >= 32 || z < 0 || z >= 32) return null;
        try {
            return mcc$modern.getChunkDataInputStream(new net.minecraft.world.level.ChunkPos(x, z));
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    public boolean isChunkSaved(int x, int z) {
        return mcc$modern != null && x >= 0 && x < 32 && z >= 0 && z < 32 && mcc$modern.hasChunk(new net.minecraft.world.level.ChunkPos(x, z));
    }

    public void close() throws java.io.IOException {
        if (mcc$modern != null) mcc$modern.close();
    }

    public File mcc$file() {
        return fileName;
    }
}
