package org.kylepitcock.legacyconnector.vb1_7_3.client;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173.*;

class InventoryControlPositionsB173Test {
    @Test void csvRoundtripKeepsBothAttachedAndInventoryRelativeFreeIntent() {
        Anchor fallback = InventoryControlPositionsB173.defaults("offhand");
        for (Edge edge : Edge.values()) for (double along : new double[] {0, .05, .25, .5, 1}) {
            var anchor = new Anchor(edge, along, -173, 89);
            assertEquals(anchor, InventoryControlPositionsB173.parse(InventoryControlPositionsB173.encode(anchor), fallback));
        }
        var extremes = new Anchor(Edge.FREE, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(extremes, InventoryControlPositionsB173.parse(InventoryControlPositionsB173.encode(extremes), fallback));
    }

    @Test void malformedPropertiesUseIndependentLeftEdgeDefaults() {
        Anchor offhand = InventoryControlPositionsB173.defaults("offhand"), book = InventoryControlPositionsB173.defaults("recipeBook");
        assertEquals(new Anchor(Edge.LEFT, .05, 0, 0), offhand);
        assertEquals(new Anchor(Edge.LEFT, .25, 0, 0), book);
        for (String raw : new String[] {"", "LEFT,.5,0", "UNKNOWN,.5,0,0", "LEFT,NaN,0,0", "LEFT,Infinity,0,0",
                "LEFT,-.1,0,0", "LEFT,1.01,0,0", "FREE,0,2147483648,0", "FREE,0,0,notanumber", "LEFT,.5,0,0,extra"}) {
            assertSame(offhand, InventoryControlPositionsB173.parse(raw, offhand));
            assertSame(book, InventoryControlPositionsB173.parse(raw, book));
        }
        assertSame(book, InventoryControlPositionsB173.parse(null, book));
        assertThrows(IllegalArgumentException.class, () -> InventoryControlPositionsB173.defaults("other"));
        assertThrows(IllegalArgumentException.class, () -> InventoryControlPositionsB173.encode(new Anchor(Edge.LEFT, Double.NaN, 0, 0)));
    }
}
