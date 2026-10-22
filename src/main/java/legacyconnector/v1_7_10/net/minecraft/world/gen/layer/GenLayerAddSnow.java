package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

/** Assigns climates to land: 1 warm (most), 3 cool, 4 icy. */
public class GenLayerAddSnow extends GenLayerNeighbors {
    public GenLayerAddSnow(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        if (isBiomeOceanic(c)) return c;
        int r = nextInt(6);
        return r == 0 ? 4 : r <= 1 ? 3 : 1;
    }
}
