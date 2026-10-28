package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import legacyconnector.v1_12_2.net.minecraft.init.Biomes;

/** Sunflower plains: one plains cell in 57 becomes its mutated variant (id + 128). */
public class GenLayerRareBiome extends GenLayer {
    public GenLayerRareBiome(long seed, GenLayer parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int[] in = parent.getInts(x - 1, z - 1, w + 2, h + 2);
        int[] out = IntCache.getIntCache(w * h);
        int plains = id(Biomes.PLAINS);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                initChunkSeed(j + x, i + z);
                int k = in[j + 1 + (i + 1) * (w + 2)];
                out[j + i * w] = nextInt(57) == 0 && k == plains ? plains + 128 : k;
            }
        }
        return out;
    }
}
