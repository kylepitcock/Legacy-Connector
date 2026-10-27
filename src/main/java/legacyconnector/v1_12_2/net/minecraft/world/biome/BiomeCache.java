package legacyconnector.v1_12_2.net.minecraft.world.biome;

/**
 * 1.12.2 per-chunk cache of a biome provider's per-block biomes (16x16 per chunk), dropped after 15 seconds unused. Thread-safe
 * (world generation runs on several threads).
 */
public class BiomeCache {
    private final BiomeProvider provider;
    private final java.util.Map<Long, Block> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile long lastCleanupTime;

    public BiomeCache(BiomeProvider provider) {
        this.provider = provider;
    }

    private Block entry(int x, int z) {
        int cx = x >> 4, cz = z >> 4;
        long key = (long) cx & 4294967295L | ((long) cz & 4294967295L) << 32;
        Block b = cache.get(key);
        if (b == null) {
            b = new Block(cx, cz);
            cache.put(key, b);
        }
        b.lastAccessTime = System.currentTimeMillis();
        return b;
    }

    public Biome getBiome(int x, int z, Biome defaultValue) {
        Biome b = entry(x, z).biomes[(x & 15) | (z & 15) << 4];
        return b == null ? defaultValue : b;
    }

    public void cleanupCache() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < 7500L) return;
        lastCleanupTime = now;
        cache.values().removeIf(b -> now - b.lastAccessTime > 30000L);
    }

    public Biome[] getCachedBiomes(int x, int z) {
        return entry(x, z).biomes;
    }

    public class Block {
        public final Biome[] biomes = new Biome[256];
        public final int x;
        public final int z;
        public volatile long lastAccessTime;

        public Block(int x, int z) {
            this.x = x;
            this.z = z;
            provider.getBiomes(biomes, x << 4, z << 4, 16, 16, false);
        }
    }
}
