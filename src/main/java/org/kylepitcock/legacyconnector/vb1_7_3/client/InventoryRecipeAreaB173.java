package org.kylepitcock.legacyconnector.vb1_7_3.client;

/**
 * Where a re-laid-out player inventory draws its crafting arrow. Recipe viewers keep their "show recipes" hotspot at the vanilla arrow
 * (137,29 sized 10x13 from the inventory's corner); the arrow of an original layout sits between its own crafting grid and result slot.
 */
public final class InventoryRecipeAreaB173 {
    private InventoryRecipeAreaB173() {}
    /** Vanilla hotspot: top-left and size relative to the inventory's corner, and the arrow centre it implies. */
    public static final double VANILLA_CENTER_X = 137 + 10 / 2.0, VANILLA_CENTER_Y = 29 + 13 / 2.0;

    /** Item positions (16x16, frame at -1) of the crafting grid and the result slot, relative to the inventory's corner. */
    public static double[] arrowCenter(int[][] grid, int[] result) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int[] slot : grid) {
            minX = Math.min(minX, slot[0] - 1); minY = Math.min(minY, slot[1] - 1);
            maxX = Math.max(maxX, slot[0] + 17); maxY = Math.max(maxY, slot[1] + 17);
        }
        double gx = (minX + maxX) / 2, gy = (minY + maxY) / 2, rx = result[0] + 8, ry = result[1] + 8;
        if (Math.abs(rx - gx) >= Math.abs(ry - gy)) {
            double edge = rx > gx ? maxX : minX, facing = rx > gx ? result[0] - 1 : result[0] + 17;
            return new double[] {(edge + facing) / 2, (gy + ry) / 2};
        }
        double edge = ry > gy ? maxY : minY, facing = ry > gy ? result[1] - 1 : result[1] + 17;
        return new double[] {(gx + rx) / 2, (edge + facing) / 2};
    }

    /** How far the vanilla hotspot must move to sit on the layout's arrow; {0,0} for the vanilla layout. */
    public static int[] shift(int[][] grid, int[] result) {
        double[] center = arrowCenter(grid, result);
        int dx = (int) Math.round(center[0] - VANILLA_CENTER_X), dy = (int) Math.round(center[1] - VANILLA_CENTER_Y);
        // The estimate can sit a pixel off the real vanilla arrow; that is still the vanilla layout.
        return Math.abs(dx) <= 1 && Math.abs(dy) <= 1 ? new int[] {0, 0} : new int[] {dx, dy};
    }
}
