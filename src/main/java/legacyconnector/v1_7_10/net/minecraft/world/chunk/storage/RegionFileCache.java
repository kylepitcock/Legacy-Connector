package legacyconnector.v1_7_10.net.minecraft.world.chunk.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** The open region files by path (one file object per region, as 1.7.10 kept them). */
public class RegionFileCache {
    private static final Map<File, RegionFile> regionsByFilename = new HashMap<>();

    public static synchronized RegionFile createOrLoadRegionFile(File worldDir, int chunkX, int chunkZ) {
        File dir = new File(worldDir, "region");
        File file = new File(dir, "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mca");
        RegionFile region = regionsByFilename.get(file);
        if (region != null) return region;
        if (!dir.exists()) dir.mkdirs();
        if (regionsByFilename.size() >= 256) clearRegionFileReferences();
        region = new RegionFile(file);
        regionsByFilename.put(file, region);
        return region;
    }

    public static synchronized void clearRegionFileReferences() {
        for (RegionFile region : regionsByFilename.values()) {
            try {
                if (region != null) region.close();
            } catch (IOException ignored) {
            }
        }
        regionsByFilename.clear();
    }

    public static DataInputStream getChunkInputStream(File worldDir, int chunkX, int chunkZ) {
        return createOrLoadRegionFile(worldDir, chunkX, chunkZ).getChunkDataInputStream(chunkX & 31, chunkZ & 31);
    }

    public static DataOutputStream getChunkOutputStream(File worldDir, int chunkX, int chunkZ) {
        return createOrLoadRegionFile(worldDir, chunkX, chunkZ).getChunkDataOutputStream(chunkX & 31, chunkZ & 31);
    }
}
