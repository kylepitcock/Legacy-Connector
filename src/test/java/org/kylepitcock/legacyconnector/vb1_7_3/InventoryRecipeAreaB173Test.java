package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.vb1_7_3.client.InventoryRecipeAreaB173;

class InventoryRecipeAreaB173Test {
    @Test void vanillaLayoutNeedsNoShift() {
        int[][] grid = {{98, 18}, {116, 18}, {98, 36}, {116, 36}};
        assertArrayEquals(new int[] {0, 0}, InventoryRecipeAreaB173.shift(grid, new int[] {154, 28}));
    }
    @Test void gridAboveResultPutsTheArrowBetweenThemAndShiftsTheHotspot() {
        int[][] grid = {{100, 20}, {118, 20}, {100, 38}, {118, 38}};
        var center = InventoryRecipeAreaB173.arrowCenter(grid, new int[] {109, 70});
        assertEquals(117.0, center[0], 1.0);
        assertTrue(center[1] > 56 && center[1] < 70, "between grid bottom and result: " + center[1]);
        var shift = InventoryRecipeAreaB173.shift(grid, new int[] {109, 70});
        assertTrue(shift[0] < 0 && shift[1] > 0);
    }
}
