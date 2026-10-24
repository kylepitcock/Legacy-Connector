package legacyconnector.v1_7_10.net.minecraft.util;

/** The six block faces, in 1.7.10's order (the side numbers legacy code uses everywhere). */
public enum EnumFacing {
    DOWN(0, 1, 0, -1, 0),
    UP(1, 0, 0, 1, 0),
    NORTH(2, 3, 0, 0, -1),
    SOUTH(3, 2, 0, 0, 1),
    WEST(4, 5, -1, 0, 0),
    EAST(5, 4, 1, 0, 0);

    private final int index;
    private final int opposite;
    private final int dx;
    private final int dy;
    private final int dz;

    EnumFacing(int index, int opposite, int dx, int dy, int dz) {
        this.index = index;
        this.opposite = opposite;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public int getIndex() {
        return index;
    }

    public int getFrontOffsetX() {
        return dx;
    }

    public int getFrontOffsetY() {
        return dy;
    }

    public int getFrontOffsetZ() {
        return dz;
    }

    public EnumFacing getOpposite() {
        return values()[opposite];
    }

    public static EnumFacing getFront(int index) {
        return values()[Math.abs(index % values().length)];
    }
}
