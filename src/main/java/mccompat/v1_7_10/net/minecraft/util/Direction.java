package mccompat.v1_7_10.net.minecraft.util;

/** Horizontal direction tables (0 south, 1 west, 2 north, 3 east). */
public class Direction {
    public static final int[] offsetX = {0, -1, 0, 1};
    public static final int[] offsetZ = {1, 0, -1, 0};
    public static final String[] directions = {"SOUTH", "WEST", "NORTH", "EAST"};
    public static final int[] directionToFacing = {3, 4, 2, 5};
    public static final int[] facingToDirection = {-1, -1, 2, 0, 1, 3};
    public static final int[] rotateOpposite = {2, 3, 0, 1};
    public static final int[] rotateRight = {1, 2, 3, 0};
    public static final int[] rotateLeft = {3, 0, 1, 2};
    public static final int[][] bedDirection = {{1, 0, 3, 2, 5, 4}, {1, 0, 5, 4, 2, 3}, {1, 0, 2, 3, 4, 5}, {1, 0, 4, 5, 3, 2}};

    public static int getMovementDirection(double dx, double dz) {
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0.0 ? 1 : 3;
        return dz > 0.0 ? 2 : 0;
    }
}
