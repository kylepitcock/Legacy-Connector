package legacyconnector.v1_7_10.net.minecraft.world.biome;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;
import legacyconnector.v1_7_10.net.minecraft.world.gen.layer.GenLayer;
import legacyconnector.v1_7_10.net.minecraft.world.gen.layer.IntCache;

/** 1.7.10 biome provider over a GenLayer stack. Mods subclass it for their world types. */
public class WorldChunkManager {
    public GenLayer genBiomes;
    public GenLayer biomeIndexLayer;
    public List<BiomeGenBase> biomesToSpawnIn = new ArrayList<>(List.of(BiomeGenBase.forest, BiomeGenBase.plains, BiomeGenBase.taiga,
            BiomeGenBase.taigaHills, BiomeGenBase.forestHills, BiomeGenBase.jungle, BiomeGenBase.jungleHills));

    protected WorldChunkManager() {
    }

    public WorldChunkManager(long seed, WorldType worldType) {
        this();
        GenLayer[] layers = getModdedBiomeGenerators(worldType, seed, GenLayer.initializeAllBiomeGenerators(seed, worldType));
        this.genBiomes = layers[0];
        this.biomeIndexLayer = layers[1];
    }

    public WorldChunkManager(World world) {
        this(world == null ? 0L : world.getSeed(), WorldType.DEFAULT);
    }

    public GenLayer[] getModdedBiomeGenerators(WorldType worldType, long seed, GenLayer[] original) {
        return original;
    }

    public List<BiomeGenBase> getBiomesToSpawnIn() {
        return biomesToSpawnIn;
    }

    /**
     * Single-column lookups read a 16x16 block of columns computed once, like vanilla's BiomeCache did. Evaluating the layer
     * stack for one column costs about as much as for a whole block, and the modern biome source asks per 4x4 cell.
     */
    private final java.util.Map<Long, BiomeGenBase[]> mcc$columnCache = new java.util.HashMap<>();
    private static final int MCC$CACHED_BLOCKS = 2048;

    public BiomeGenBase getBiomeGenAt(int x, int z) {
        long key = ((long) (x >> 4) << 32) | ((z >> 4) & 0xFFFFFFFFL);
        BiomeGenBase[] block;
        synchronized (mcc$columnCache) {
            block = mcc$columnCache.get(key);
            if (block == null) {
                if (mcc$columnCache.size() >= MCC$CACHED_BLOCKS) mcc$columnCache.clear();
                int[] ids = biomeIndexLayer.getInts(x & ~15, z & ~15, 16, 16);
                block = new BiomeGenBase[256];
                for (int i = 0; i < 256; i++) block[i] = BiomeGenBase.mcc$biomeOrOcean(ids[i]);
                mcc$columnCache.put(key, block);
            }
        }
        return block[(z & 15) * 16 + (x & 15)];
    }

    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] out, int x, int z, int w, int h) {
        IntCache.resetIntCache();
        if (out == null || out.length < w * h) out = new BiomeGenBase[w * h];
        int[] ids = genBiomes.getInts(x, z, w, h);
        for (int i = 0; i < w * h; i++) out[i] = BiomeGenBase.mcc$biomeOrOcean(ids[i]);
        return out;
    }

    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] out, int x, int z, int w, int h) {
        return getBiomeGenAt(out, x, z, w, h, true);
    }

    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] out, int x, int z, int w, int h, boolean cache) {
        if (out == null || out.length < w * h) out = new BiomeGenBase[w * h];
        int[] ids = biomeIndexLayer.getInts(x, z, w, h);
        for (int i = 0; i < w * h; i++) out[i] = BiomeGenBase.mcc$biomeOrOcean(ids[i]);
        return out;
    }

    public float[] getRainfall(float[] out, int x, int z, int w, int h) {
        if (out == null || out.length < w * h) out = new float[w * h];
        int[] ids = biomeIndexLayer.getInts(x, z, w, h);
        for (int i = 0; i < w * h; i++) out[i] = Math.min(1.0F, BiomeGenBase.mcc$biomeOrOcean(ids[i]).getIntRainfall() / 65536.0F);
        return out;
    }

    public float getTemperatureAtHeight(float temperature, int height) {
        return temperature;
    }

    public boolean areBiomesViable(int x, int z, int radius, List<?> allowed) {
        int x0 = x - radius >> 2;
        int z0 = z - radius >> 2;
        int x1 = x + radius >> 2;
        int z1 = z + radius >> 2;
        int[] ids = genBiomes.getInts(x0, z0, x1 - x0 + 1, z1 - z0 + 1);
        for (int id : ids) if (!allowed.contains(BiomeGenBase.mcc$biomeOrOcean(id))) return false;
        return true;
    }

    public void cleanupCache() {
    }

    /** A world's biomes as a chunk manager (World.getWorldChunkManager): every query reads the actual biomes. */
    public static final class WorldView extends WorldChunkManager {
        private final World world;

        public WorldView(World world) {
            this.world = world;
        }

        @Override
        public BiomeGenBase getBiomeGenAt(int x, int z) {
            return world.getBiomeGenForCoords(x, z);
        }

        @Override
        public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] out, int x, int z, int w, int h) {
            if (out == null || out.length < w * h) out = new BiomeGenBase[w * h];
            for (int i = 0; i < h; i++) for (int j = 0; j < w; j++) out[j + i * w] = world.getBiomeGenForCoords((x + j) << 2, (z + i) << 2);
            return out;
        }

        @Override
        public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] out, int x, int z, int w, int h, boolean cache) {
            if (out == null || out.length < w * h) out = new BiomeGenBase[w * h];
            for (int i = 0; i < h; i++) for (int j = 0; j < w; j++) out[j + i * w] = world.getBiomeGenForCoords(x + j, z + i);
            return out;
        }

        @Override
        public float[] getRainfall(float[] out, int x, int z, int w, int h) {
            if (out == null || out.length < w * h) out = new float[w * h];
            BiomeGenBase[] biomes = getBiomeGenAt(null, x, z, w, h, false);
            for (int i = 0; i < w * h; i++) out[i] = Math.min(1.0F, biomes[i].getIntRainfall() / 65536.0F);
            return out;
        }

        @Override
        public boolean areBiomesViable(int x, int z, int radius, List<?> allowed) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (!allowed.contains(world.getBiomeGenForCoords(x + dx, z + dz))) return false;
                }
            }
            return true;
        }
    }
}
