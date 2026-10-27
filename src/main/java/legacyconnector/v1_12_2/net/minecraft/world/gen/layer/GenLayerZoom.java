package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

/** Doubles the resolution of its parent; new cells take a neighbour's value (mode, else random). */
public class GenLayerZoom extends GenLayer {
    public GenLayerZoom(long seed, GenLayer parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int px = x >> 1;
        int pz = z >> 1;
        int pw = (w >> 1) + 2;
        int ph = (h >> 1) + 2;
        int[] in = parent.getInts(px, pz, pw, ph);
        int zw = pw - 1 << 1;
        int zh = ph - 1 << 1;
        int[] zoomed = IntCache.getIntCache(zw * zh);
        for (int i = 0; i < ph - 1; i++) {
            int row = (i << 1) * zw;
            int a = in[i * pw];
            int c = in[(i + 1) * pw];
            for (int j = 0; j < pw - 1; j++) {
                initChunkSeed((long) (j + px) << 1, (long) (i + pz) << 1);
                int b = in[j + 1 + i * pw];
                int d = in[j + 1 + (i + 1) * pw];
                zoomed[row] = a;
                zoomed[row++ + zw] = selectRandom(a, c);
                zoomed[row] = selectRandom(a, b);
                zoomed[row++ + zw] = selectModeOrRandom(a, b, c, d);
                a = b;
                c = d;
            }
        }
        int[] out = IntCache.getIntCache(w * h);
        for (int i = 0; i < h; i++) System.arraycopy(zoomed, (i + (z & 1)) * zw + (x & 1), out, i * w, w);
        return out;
    }

    public static GenLayer magnify(long seed, GenLayer layer, int times) {
        GenLayer out = layer;
        for (int i = 0; i < times; i++) out = new GenLayerZoom(seed + i, out);
        return out;
    }
}
