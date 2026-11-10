package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DimensionTravelB173Test {
    @Test void nestedPortalsKeepOriginalPushPopAndSameDestinationFallback() {
        var history = new LinkedList<Integer>();
        assertEquals(4, DimensionsB173.selectDestination(history, 0, 4));
        assertEquals(List.of(4), history);
        assertEquals(7, DimensionsB173.selectDestination(history, 4, 7));
        assertEquals(List.of(7, 4), history);
        // Original pops the current destination first, then converts that same-realm result to Overworld.
        assertEquals(0, DimensionsB173.selectDestination(history, 7, 0));
        assertEquals(List.of(4), history);
        assertEquals(4, DimensionsB173.selectDestination(history, 0, 0));
        assertTrue(history.isEmpty());
    }

    @Test void matchingPortalReturnsHomeAndHistoriesRemainIndependent() {
        var first = new LinkedList<Integer>();
        var second = new LinkedList<Integer>();
        assertEquals(-1, DimensionsB173.selectDestination(first, 0, -1));
        assertEquals(1, DimensionsB173.selectDestination(second, 0, 1));
        assertEquals(0, DimensionsB173.selectDestination(first, -1, -1));
        assertTrue(first.isEmpty());
        assertEquals(List.of(1), second);
        assertEquals(0, DimensionsB173.selectDestination(second, 1, 1));
        assertTrue(second.isEmpty());
    }
}
