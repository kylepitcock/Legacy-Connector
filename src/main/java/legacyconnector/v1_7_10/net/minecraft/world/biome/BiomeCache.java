package legacyconnector.v1_7_10.net.minecraft.world.biome;

import java.util.concurrent.ConcurrentHashMap;

/** Caches the biomes of 16x16 areas of a chunk manager, as vanilla's did (mods' managers read it per block). */
public class BiomeCache {
    private final WorldChunkManager chunkManager;
    private final ConcurrentHashMap<Long, BiomeGenBase[]> blocks = new ConcurrentHashMap<>();

    public BiomeCache(WorldChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    public BiomeGenBase[] getCachedBiomes(int x, int z) {
        int bx = x >> 4, bz = z >> 4;
        long key = (long) bx & 0xFFFFFFFFL | ((long) bz & 0xFFFFFFFFL) << 32;
        BiomeGenBase[] biomes = blocks.get(key);
        if (biomes == null) {
            biomes = chunkManager.getBiomeGenAt(null, bx << 4, bz << 4, 16, 16, false);
            if (blocks.size() > 4096) blocks.clear();
            blocks.put(key, biomes);
        }
        return biomes;
    }

    public BiomeGenBase getBiomeGenAt(int x, int z) {
        return getCachedBiomes(x, z)[x & 15 | (z & 15) << 4];
    }

    public void cleanupCache() {
        if (blocks.size() > 1024) blocks.clear();
    }
}
