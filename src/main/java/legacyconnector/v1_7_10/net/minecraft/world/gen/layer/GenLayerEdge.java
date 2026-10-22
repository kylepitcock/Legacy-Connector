package legacyconnector.v1_7_10.net.minecraft.world.gen.layer;

/** Climate transitions: warm next to cold becomes temperate, icy next to warm becomes cool, plus rare special flags. */
public class GenLayerEdge extends GenLayerNeighbors {
    public enum Mode {
        COOL_WARM, HEAT_ICE, SPECIAL
    }

    private final Mode mode;

    public GenLayerEdge(long seed, GenLayer parent, Mode mode) {
        super(seed, parent);
        this.mode = mode;
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int nw, int ne, int sw, int se) {
        switch (mode) {
            case COOL_WARM -> {
                if (c == 1 && (n == 3 || e == 3 || w == 3 || s == 3 || n == 4 || e == 4 || w == 4 || s == 4)) return 2;
                return c;
            }
            case HEAT_ICE -> {
                if (c == 4 && (n == 1 || e == 1 || w == 1 || s == 1 || n == 2 || e == 2 || w == 2 || s == 2)) return 3;
                return c;
            }
            default -> {
                if (c != 0 && nextInt(13) == 0) c |= 1 + nextInt(15) << 8 & 3840;
                return c;
            }
        }
    }
}
