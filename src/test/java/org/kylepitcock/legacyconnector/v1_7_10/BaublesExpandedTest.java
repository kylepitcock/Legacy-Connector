package org.kylepitcock.legacyconnector.v1_7_10;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import legacyconnector.v1_7_10.baubles.api.BaubleType;
import legacyconnector.v1_7_10.baubles.api.expanded.BaubleExpandedSlots;
import org.junit.jupiter.api.Test;

/** The Baubles Expanded slot table of the Curios-backed Baubles view. */
class BaublesExpandedTest {
    @Test
    void originalTypesKeepTheirOrdinals() {
        assertEquals(0, BaubleType.RING.ordinal());
        assertEquals(1, BaubleType.AMULET.ordinal());
        assertEquals(2, BaubleType.BELT.ordinal());
    }

    @Test
    void typesAndSlotsAgree() {
        assertArrayEquals(new int[] {1, 2}, BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType("ring"));
        assertArrayEquals(new int[] {7}, BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType("trinket"));
        assertEquals(8, BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType("universal").length);
        for (BaubleType t : BaubleType.values()) {
            for (int slot : t.getValidSlots()) {
                if (t != BaubleType.UNIVERSAL) assertEquals(t.getTypeName(), BaubleExpandedSlots.getSlotType(slot));
            }
        }
        assertEquals("invalid", BaubleExpandedSlots.getSlotType(99));
    }

    @Test
    void ownTypesAreNotRegistered() {
        assertTrue(BaubleExpandedSlots.tryRegisterType("charm"));
        assertFalse(BaubleExpandedSlots.tryRegisterType("quiver"));
        assertTrue(BaubleExpandedSlots.tryAssignSlotsUpToMinimum("ring", 2));
        assertFalse(BaubleExpandedSlots.tryAssignSlotsUpToMinimum("ring", 3));
    }
}
