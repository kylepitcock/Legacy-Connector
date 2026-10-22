package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

/** Removes single-cell noise: a cell between two equal neighbours takes their value. */
public class GenLayerSmooth extends GenLayerNeighbors {
    public GenLayerSmooth(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        if (w == e && n == s) return nextInt(2) == 0 ? w : n;
        if (w == e) return w;
        if (n == s) return n;
        return c;
    }
}
