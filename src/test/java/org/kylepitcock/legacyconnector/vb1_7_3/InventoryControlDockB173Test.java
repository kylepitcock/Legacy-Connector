package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173.*;

class InventoryControlDockB173Test {
    private static final Rect INVENTORY = new Rect(100, 80, 176, 166);
    private static Anchor anchor(Edge edge, double along) { return new Anchor(edge, along, 0, 0); }
    private static Placement resolve(Anchor anchor, List<Rect> obstacles) {
        return InventoryControlDockB173.resolve(anchor, INVENTORY, 26, 500, 400, obstacles);
    }
    private static boolean overlaps(Rect a, Rect b) {
        return a.x() < (long) b.x() + b.width() && (long) a.x() + a.width() > b.x()
                && a.y() < (long) b.y() + b.height() && (long) a.y() + a.height() > b.y();
    }
    private static void safe(Placement placement, Rect inventory, int width, int height, List<Rect> blockers) {
        assertNotNull(placement);Rect r = placement.bounds();
        assertTrue(r.x() >= 0 && r.y() >= 0 && (long) r.x() + r.width() <= width && (long) r.y() + r.height() <= height);
        for (Rect blocker : blockers) assertFalse(overlaps(r, blocker));
        if (overlaps(r, inventory)) {
            long x = Math.min((long) r.x() + r.width(), (long) inventory.x() + inventory.width()) - Math.max(r.x(), inventory.x());
            long y = Math.min((long) r.y() + r.height(), (long) inventory.y() + inventory.height()) - Math.max(r.y(), inventory.y());
            assertTrue(Math.min(x, y) <= 4);
            assertTrue(r.x() < inventory.x() || (long) r.x() + r.width() > (long) inventory.x() + inventory.width()
                    || r.y() < inventory.y() || (long) r.y() + r.height() > (long) inventory.y() + inventory.height());
        }
    }

    @Test void attachedControlsFollowNormalizedStraightEdgesAcrossRecenteringAndSizeChanges() {
        for (Edge edge : List.of(Edge.LEFT, Edge.RIGHT, Edge.TOP, Edge.BOTTOM)) {
            Anchor saved = anchor(edge, .5);Placement first = resolve(saved, List.of());
            safe(first, INVENTORY, 500, 400, List.of());assertEquals(edge, first.edge());
            var shiftedInventory = new Rect(180, 120, 176, 166);
            Placement shifted = InventoryControlDockB173.resolve(saved, shiftedInventory, 26, 800, 600, List.of());
            assertEquals(first.bounds().x() + 80, shifted.bounds().x());assertEquals(first.bounds().y() + 40, shifted.bounds().y());
            Placement larger = InventoryControlDockB173.resolve(saved, INVENTORY, 32, 500, 400, List.of());
            safe(larger, INVENTORY, 500, 400, List.of());assertEquals(edge, larger.edge());
            if (edge == Edge.LEFT || edge == Edge.RIGHT) {
                assertEquals(first.bounds().y() + 13, larger.bounds().y() + 16);
                assertTrue(first.bounds().y() >= INVENTORY.y());
                assertTrue(first.bounds().y() + 26 <= INVENTORY.y() + INVENTORY.height());
            } else assertEquals(first.bounds().x() + 13, larger.bounds().x() + 16);
        }
    }

    @Test void edgeEndpointsAlignFlushWithInventoryCornersAndSurviveDrops() {
        for (Edge edge : List.of(Edge.LEFT, Edge.RIGHT, Edge.TOP, Edge.BOTTOM)) {
            for (int endpoint : new int[] {0, 1}) {
                Anchor saved = anchor(edge, endpoint);
                Placement placement = resolve(saved, List.of());
                safe(placement, INVENTORY, 500, 400, List.of());
                assertEquals(edge, placement.edge());
                Rect r = placement.bounds();
                if (edge == Edge.LEFT || edge == Edge.RIGHT) {
                    assertEquals(INVENTORY.y() + endpoint * (INVENTORY.height() - 26), r.y());
                    assertEquals(edge == Edge.LEFT ? INVENTORY.x() - 22 : INVENTORY.x() + INVENTORY.width() - 4, r.x());
                } else {
                    assertEquals(INVENTORY.x() + endpoint * (INVENTORY.width() - 26), r.x());
                    assertEquals(edge == Edge.TOP ? INVENTORY.y() - 22 : INVENTORY.y() + INVENTORY.height() - 4, r.y());
                }
                Anchor dropped = InventoryControlDockB173.drop(r, INVENTORY, 26, 500, 400, List.of());
                assertEquals(saved, dropped);
                assertEquals(placement, resolve(dropped, List.of()));
                Placement obstructed = resolve(saved, List.of(r));
                safe(obstructed, INVENTORY, 500, 400, List.of(r));
                assertEquals(edge, obstructed.edge());
                assertNotEquals(r, obstructed.bounds());
            }
        }
    }

    @Test void obstaclesSlideAlongTheSavedEdgeAndFallbackIsTemporary() {
        Anchor saved = anchor(Edge.LEFT, .5);Placement preferred = resolve(saved, List.of());
        List<Rect> blocker = List.of(preferred.bounds());
        Placement slid = resolve(saved, blocker);safe(slid, INVENTORY, 500, 400, blocker);
        assertEquals(Edge.LEFT, slid.edge());assertNotEquals(preferred.bounds(), slid.bounds());
        List<Rect> entireLeft = List.of(new Rect(0, 0, 104, 400));
        Placement fallback = resolve(saved, entireLeft);safe(fallback, INVENTORY, 500, 400, entireLeft);
        assertNotEquals(Edge.LEFT, fallback.edge());assertEquals(anchor(Edge.LEFT, .5), saved);
        assertEquals(preferred, resolve(saved, List.of()));
        Placement offhand = resolve(anchor(Edge.LEFT, .05), List.of());
        Placement book = resolve(anchor(Edge.LEFT, .25), List.of(offhand.bounds()));
        safe(book, INVENTORY, 500, 400, List.of(offhand.bounds()));
    }

    @Test void dropsSnapWithinEightPixelsAndFarFreePositionsStayRelative() {
        Anchor snapped = InventoryControlDockB173.drop(new Rect(70, 130, 26, 26), INVENTORY, 26, 500, 400, List.of());
        assertEquals(Edge.LEFT, snapped.edge());assertEquals(78, resolve(snapped, List.of()).bounds().x());
        Anchor outsideSnap = InventoryControlDockB173.drop(new Rect(69, 130, 26, 26), INVENTORY, 26, 500, 400, List.of());
        assertEquals(Edge.FREE, outsideSnap.edge());assertEquals(69, resolve(outsideSnap, List.of()).bounds().x());
        Anchor free = InventoryControlDockB173.drop(new Rect(20, 300, 26, 26), INVENTORY, 26, 500, 400, List.of());
        assertEquals(new Anchor(Edge.FREE, 0, -80, 220), free);
        var moved = new Rect(170, 100, 176, 166);
        Placement repositioned = InventoryControlDockB173.resolve(free, moved, 26, 700, 500, List.of());
        assertEquals(new Rect(90, 320, 26, 26), repositioned.bounds());
        Anchor inside = InventoryControlDockB173.drop(new Rect(170, 130, 26, 26), INVENTORY, 26, 500, 400, List.of());
        safe(resolve(inside, List.of()), INVENTORY, 500, 400, List.of());
        Anchor outsideViewport = InventoryControlDockB173.drop(new Rect(-100, -100, 26, 26), INVENTORY, 26, 500, 400, List.of());
        safe(resolve(outsideViewport, List.of()), INVENTORY, 500, 400, List.of());
    }

    @Test void constrainedViewportsAndImpossiblePlacementsNeverProduceUnsafeCoordinates() {
        var nearLeft = new Rect(3, 70, 176, 166);
        var saved = anchor(Edge.LEFT, .05);
        var placement = InventoryControlDockB173.resolve(saved, nearLeft, 26, 320, 300, List.of());
        safe(placement, nearLeft, 320, 300, List.of());assertNotEquals(Edge.LEFT, placement.edge());
        assertNull(resolve(saved, List.of(new Rect(0, 0, 500, 400))));
        assertNull(InventoryControlDockB173.resolve(saved, INVENTORY, 26, 20, 400, List.of()));
        assertNull(resolve(new Anchor(Edge.TOP, Double.NaN, 0, 0), List.of()));
        assertNull(resolve(new Anchor(null, .5, 0, 0), List.of()));
    }

    @Test void variedBlockersKeepBothResolvedAndCommittedPositionsSafe() {
        var random = new Random(173);
        int resolved = 0;
        for (int i = 0; i < 200; i++) {
            var inventory = new Rect(20 + random.nextInt(260), 20 + random.nextInt(180), 176, 166);
            var blockers = new ArrayList<Rect>();
            for (int j = 0; j < 6; j++) blockers.add(new Rect(random.nextInt(470), random.nextInt(370), 30, 30));
            Edge edge = Edge.values()[random.nextInt(5)];
            var anchor = new Anchor(edge, random.nextDouble(), random.nextInt(500) - 250, random.nextInt(400) - 200);
            Placement placement = InventoryControlDockB173.resolve(anchor, inventory, 26, 500, 400, blockers);
            if (placement == null) continue;
            resolved++;safe(placement, inventory, 500, 400, blockers);
            Anchor dropped = InventoryControlDockB173.drop(placement.bounds(), inventory, 26, 500, 400, blockers);
            assertNotNull(dropped);
            safe(InventoryControlDockB173.resolve(dropped, inventory, 26, 500, 400, blockers), inventory, 500, 400, blockers);
        }
        assertTrue(resolved > 150);
    }
}
