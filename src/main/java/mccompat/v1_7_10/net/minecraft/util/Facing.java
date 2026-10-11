package mccompat.v1_7_10.net.minecraft.util;

/** Side tables (0 down, 1 up, 2 north, 3 south, 4 west, 5 east). */
public class Facing {
    public static final int[] oppositeSide = {1, 0, 3, 2, 5, 4};
    public static final int[] offsetsXForSide = {0, 0, 0, 0, -1, 1};
    public static final int[] offsetsYForSide = {-1, 1, 0, 0, 0, 0};
    public static final int[] offsetsZForSide = {0, 0, -1, 1, 0, 0};
    public static final String[] facings = {"DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST"};
}
