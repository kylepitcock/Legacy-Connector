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

    public BiomeGenBase getBiomeGenAt(int x, int z) {
        return biomes[x & 15 | (z & 15) << 4];
    }
}
