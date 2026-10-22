package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

/** Base for layers that look at a cell and its neighbours in the parent's map (shared plumbing, not a 1.7.10 class). */
abstract class GenLayerNeighbors extends GenLayer {
    GenLayerNeighbors(long seed, GenLayer parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int pw = w + 2;
        int[] in = parent.getInts(x - 1, z - 1, pw, h + 2);
        int[] out = IntCache.getIntCache(w * h);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                initChunkSeed(j + x, i + z);
                int c = in[j + 1 + (i + 1) * pw];
                out[j + i * w] = transform(c,
                        in[j + 1 + i * pw], in[j + 2 + (i + 1) * pw], in[j + (i + 1) * pw], in[j + 1 + (i + 2) * pw],
                        in[j + i * pw], in[j + 2 + i * pw], in[j + (i + 2) * pw], in[j + 2 + (i + 2) * pw]);
            }
        }
        return out;
    }

    /** n/e/w/s are the orthogonal neighbours; nw/ne/sw/se the diagonal ones. */
    abstract int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se);
}
