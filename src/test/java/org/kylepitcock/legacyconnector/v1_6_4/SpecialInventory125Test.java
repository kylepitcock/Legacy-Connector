package org.kylepitcock.legacyconnector.v1_6_4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import buildcraft.api.ISpecialInventory;
import buildcraft.api.Orientations;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.SpecialInventory125;

/** BuildCraft 3.1's ISpecialInventory is found by name on a tile and becomes an item handler with one virtual slot. */
class SpecialInventory125Test {
    static class Machine implements ISpecialInventory {
        @Override
        public boolean addItem(ItemStack stack, boolean doAdd, Orientations from) {
            return false;
        }

        @Override
        public ItemStack extractItem(boolean doRemove, Orientations from) {
            return null;
        }
    }

    @Test
    void aSpecialInventoryTileGetsAHandlerAndOthersDoNot() {
        var handler = SpecialInventory125.forTile(new Machine(), Direction.NORTH);
        assertNotNull(handler);
        assertEquals(1, handler.getSlots());
        assertNull(SpecialInventory125.forTile(new Object(), Direction.NORTH));
        assertNull(SpecialInventory125.forTile(null, null));
    }

    @Test
    void sidesMapToBuildCraftsOrientations() {
        // ZNeg is north, XNeg west, Y up; no side is Unknown
        for (var e : new Object[][] {{Direction.NORTH, "ZNeg"}, {Direction.SOUTH, "ZPos"}, {Direction.WEST, "XNeg"}, {Direction.EAST, "XPos"},
                {Direction.UP, "YPos"}, {Direction.DOWN, "YNeg"}, {null, "Unknown"}}) {
            assertEquals(e[1], orientationNameFor((Direction) e[0]));
        }
    }

    private static String orientationNameFor(Direction side) {
        try {
            var m = Class.forName("org.kylepitcock.legacyconnector.v1_6_4.bridge.Orientations125").getDeclaredMethod("name", Direction.class);
            m.setAccessible(true);
            return (String) m.invoke(null, side);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
