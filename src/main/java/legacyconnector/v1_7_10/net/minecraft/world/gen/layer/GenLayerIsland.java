package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

/** Initial land/ocean noise: 1 in 10 cells land, the origin always land. */
public class GenLayerIsland extends GenLayer {
    public GenLayerIsland(long seed) {
        super(seed);
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int[] out = IntCache.getIntCache(w * h);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                initChunkSeed(x + j, z + i);
                out[j + i * w] = nextInt(10) == 0 ? 1 : 0;
            }
        }
        if (x > -w && x <= 0 && z > -h && z <= 0) out[-x + -z * w] = 1;
        return out;
    }
}
