package legacyconnector.v1_12_2.net.minecraft.world.chunk.storage;

import java.io.DataInputStream;
import java.io.File;
import java.util.Map;

/** 1.12.2 cache of open region files of a world folder ({@code <world>/region/r.X.Z.mca}), for reading saved chunks. */
public class RegionFileCache {
    private static final Map<File, RegionFile> REGIONS_BY_FILE = new java.util.HashMap<>();

    private static File regionFile(File worldDir, int chunkX, int chunkZ) {
        return new File(new File(worldDir, "region"), "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mca");
    }

    public static synchronized RegionFile createOrLoadRegionFile(File worldDir, int chunkX, int chunkZ) {
        File file = regionFile(worldDir, chunkX, chunkZ);
        RegionFile region = REGIONS_BY_FILE.get(file);
        if (region != null) return region;
        if (REGIONS_BY_FILE.size() >= 256) clearRegionFileReferences();
        file.getParentFile().mkdirs();
        region = new RegionFile(file);
        REGIONS_BY_FILE.put(file, region);
        return region;
    }

    public static synchronized RegionFile getRegionFileIfExists(File worldDir, int chunkX, int chunkZ) {
        File file = regionFile(worldDir, chunkX, chunkZ);
        RegionFile region = REGIONS_BY_FILE.get(file);
        if (region != null) return region;
        return file.exists() ? createOrLoadRegionFile(worldDir, chunkX, chunkZ) : null;
    }

    public static synchronized void clearRegionFileReferences() {
        for (RegionFile r : REGIONS_BY_FILE.values()) {
            try {
                r.close();
            } catch (java.io.IOException e) {
                // closing a reader
            }
        }
        REGIONS_BY_FILE.clear();
    }

    /** The saved NBT stream of a chunk (modern chunk format), or null when it was never saved. */
    public static DataInputStream getChunkInputStream(File worldDir, int chunkX, int chunkZ) {
        RegionFile region = getRegionFileIfExists(worldDir, chunkX, chunkZ);
        return region == null ? null : region.getChunkDataInputStream(chunkX & 31, chunkZ & 31);
    }

    public static boolean chunkExists(File worldDir, int chunkX, int chunkZ) {
        RegionFile region = getRegionFileIfExists(worldDir, chunkX, chunkZ);
        return region != null && region.isChunkSaved(chunkX & 31, chunkZ & 31);
    }
}
