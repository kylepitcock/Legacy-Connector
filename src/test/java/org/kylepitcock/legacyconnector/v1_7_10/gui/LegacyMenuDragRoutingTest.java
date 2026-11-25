package org.kylepitcock.legacyconnector.v1_7_10.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.inventory.ClickType;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.api.LegacyVersion;

class LegacyMenuDragRoutingTest {
    @Test
    void preDragEraIsOnly147AndOlder() {
        assertTrue(LegacyMenu.isPreDragEra(LegacyVersion.V1_4_7));
        assertTrue(LegacyMenu.isPreDragEra(LegacyVersion.V1_2_5));
        assertFalse(LegacyMenu.isPreDragEra(LegacyVersion.V1_5_2));
        assertFalse(LegacyMenu.isPreDragEra(LegacyVersion.V1_6_4));
        assertFalse(LegacyMenu.isPreDragEra(LegacyVersion.V1_7_10));
        assertFalse(LegacyMenu.isPreDragEra(null));
    }

    @Test
    void dragClicksOfOldContainersRunModern() {
        assertEquals(5, ClickType.QUICK_CRAFT.ordinal());
        for (ClickType t : ClickType.values()) {
            boolean oldMode = t.ordinal() <= 3;
            assertEquals(!oldMode, LegacyMenu.mustRunModern(true, t), t.name());
            assertFalse(LegacyMenu.mustRunModern(false, t), t.name());
        }
    }
}
