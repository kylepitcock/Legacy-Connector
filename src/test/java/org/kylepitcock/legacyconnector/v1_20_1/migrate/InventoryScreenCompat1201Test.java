package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Regression tests for AlienEvo's legacy preview mouse and viewport contracts. */
class InventoryScreenCompat1201Test {
    @Test
    void convertsLegacyHorizontalAndVerticalDeltasBackToGuiCoordinates() {
        assertEquals(340.0F,
                InventoryScreenCompat1201.absoluteMouseCoordinate(360.0F, 20.0F));
        assertEquals(228.0F,
                InventoryScreenCompat1201.absoluteMouseCoordinate(351.0F, 123.0F));
    }

    @Test
    void preservesNegativeAndFractionalCursorOffsets() {
        assertEquals(404.5F,
                InventoryScreenCompat1201.absoluteMouseCoordinate(400.0F, -4.5F));
        assertEquals(72.25F,
                InventoryScreenCompat1201.absoluteMouseCoordinate(100.0F, 27.75F));
    }

    @Test
    void zeroDeltaMapsToThePreviewAnchor() {
        assertEquals(236.0F,
                InventoryScreenCompat1201.absoluteMouseCoordinate(236.0F, 0.0F));
        assertEquals(351.0F,
                InventoryScreenCompat1201.absoluteMouseCoordinate(351.0F, 0.0F));
    }

    @Test
    void derivesBadgeViewportFromTheOriginalCanvasPanelBounds() {
        assertArrayEquals(
                new int[] {78, 37, 173, 107},
                InventoryScreenCompat1201.badgeViewportBounds(12.5F, -3.25F, 0.5F, 220));
    }
}
