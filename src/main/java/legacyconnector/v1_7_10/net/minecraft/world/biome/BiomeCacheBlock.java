package legacyconnector.v1_7_10.net.minecraft.world.biome;

/** The 16x16 biome block of 1.4 to 1.6's {@code BiomeCache} (a nested class {@code BiomeCache.Block} in 1.7.10), which mods' chunk managers read directly. */
public class BiomeCacheBlock {
    public float[] temperatureValues = new float[256];
    public float[] rainfallValues = new float[256];
    public BiomeGenBase[] biomes;
    public int xPosition;
    public int zPosition;
    public long lastAccessTime;

    public BiomeCacheBlock(BiomeCache cache, int x, int z, BiomeGenBase[] biomes) {
        this.xPosition = x;
        this.zPosition = z;
        this.biomes = biomes;
    }

    /**
     * Vanilla's {@code BiomeCache.Block(cache, x, z)}: x and z are 16-block coordinates, the biomes are read from the cache's chunk manager.
     * Mods build one themselves from the reflected {@code biomeCache} field of the world's chunk manager (Tropicraft's world generator).
     */
    public BiomeCacheBlock(BiomeCache cache, int x, int z) {
        this(cache, x, z, cache.getCachedBiomes(x << 4, z << 4));
    }

    public BiomeGenBase getBiomeGenAt(int x, int z) {
        return biomes[x & 15 | (z & 15) << 4];
    }
}
