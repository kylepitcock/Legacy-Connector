package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

/** Random ids on land; rivers later run along the borders between them. */
public class GenLayerRiverInit extends GenLayer {
    public GenLayerRiverInit(long seed, GenLayer parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int[] in = parent.getInts(x, z, w, h);
        int[] out = IntCache.getIntCache(w * h);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                initChunkSeed(j + x, i + z);
                out[j + i * w] = in[j + i * w] > 0 ? nextInt(299999) + 2 : 0;
            }
        }
        return out;
    }
}
