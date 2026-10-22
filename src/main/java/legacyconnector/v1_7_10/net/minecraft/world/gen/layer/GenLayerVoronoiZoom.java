package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

/** 4x zoom from the quarter-resolution biome map to per-block biomes, with jittered cell borders. */
public class GenLayerVoronoiZoom extends GenLayer {
    public GenLayerVoronoiZoom(long seed, GenLayer parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        x -= 2;
        z -= 2;
        int px = x >> 2;
        int pz = z >> 2;
        int pw = (w >> 2) + 2;
        int ph = (h >> 2) + 2;
        int[] in = parent.getInts(px, pz, pw, ph);
        int zw = pw - 1 << 2;
        int zh = ph - 1 << 2;
        int[] zoomed = IntCache.getIntCache(zw * zh);
        for (int i = 0; i < ph - 1; i++) {
            int a = in[i * pw];
            int c = in[(i + 1) * pw];
            for (int j = 0; j < pw - 1; j++) {
                double jitter = 3.6D;
                initChunkSeed((long) (j + px) << 2, (long) (i + pz) << 2);
                double ax = (nextInt(1024) / 1024.0D - 0.5D) * jitter;
                double az = (nextInt(1024) / 1024.0D - 0.5D) * jitter;
                initChunkSeed((long) (j + px + 1) << 2, (long) (i + pz) << 2);
                double bx = (nextInt(1024) / 1024.0D - 0.5D) * jitter + 4.0D;
                double bz = (nextInt(1024) / 1024.0D - 0.5D) * jitter;
                initChunkSeed((long) (j + px) << 2, (long) (i + pz + 1) << 2);
                double cx = (nextInt(1024) / 1024.0D - 0.5D) * jitter;
                double cz = (nextInt(1024) / 1024.0D - 0.5D) * jitter + 4.0D;
                initChunkSeed((long) (j + px + 1) << 2, (long) (i + pz + 1) << 2);
                double dx = (nextInt(1024) / 1024.0D - 0.5D) * jitter + 4.0D;
                double dz = (nextInt(1024) / 1024.0D - 0.5D) * jitter + 4.0D;
                int b = in[j + 1 + i * pw];
                int d = in[j + 1 + (i + 1) * pw];
                for (int zz = 0; zz < 4; zz++) {
                    int idx = ((i << 2) + zz) * zw + (j << 2);
                    for (int xx = 0; xx < 4; xx++) {
                        double da = (zz - az) * (zz - az) + (xx - ax) * (xx - ax);
                        double db = (zz - bz) * (zz - bz) + (xx - bx) * (xx - bx);
                        double dc = (zz - cz) * (zz - cz) + (xx - cx) * (xx - cx);
                        double dd = (zz - dz) * (zz - dz) + (xx - dx) * (xx - dx);
                        int v;
                        if (da < db && da < dc && da < dd) v = a;
                        else if (db < da && db < dc && db < dd) v = b;
                        else if (dc < da && dc < db && dc < dd) v = c;
                        else v = d;
                        zoomed[idx++] = v;
                    }
                }
                a = b;
                c = d;
            }
        }
        int[] out = IntCache.getIntCache(w * h);
        for (int i = 0; i < h; i++) System.arraycopy(zoomed, (i + (z & 3)) * zw + (x & 3), out, i * w, w);
        return out;
    }
}
