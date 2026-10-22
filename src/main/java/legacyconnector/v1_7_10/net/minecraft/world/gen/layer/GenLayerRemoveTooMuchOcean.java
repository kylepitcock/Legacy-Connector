package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

/** Ocean cells surrounded by ocean become land half the time. */
public class GenLayerRemoveTooMuchOcean extends GenLayerNeighbors {
    public GenLayerRemoveTooMuchOcean(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        if (c == 0 && n == 0 && e == 0 && w == 0 && s == 0 && nextInt(2) == 0) return 1;
        return c;
    }
}
