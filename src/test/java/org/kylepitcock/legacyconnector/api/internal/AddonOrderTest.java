package org.kylepitcock.legacyconnector.api.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AddonOrderTest {
    @Test
    void parsesSingleNameAndLists() {
        assertEquals(List.of("a.B"), AddonOrder.classNames(" a.B "));
        assertEquals(List.of("a.B", "c.D"), AddonOrder.classNames(List.of("a.B", "", "c.D", "a.B")));
        assertEquals(List.of(), AddonOrder.classNames(null));
    }

    @Test
    void ordersByPriorityThenNameAndDropsDuplicates() {
        var out = AddonOrder.ordered(List.of(
                new AddonOrder.Entry("z.Late", "m1", 10),
                new AddonOrder.Entry("b.Two", "m2", 0),
                new AddonOrder.Entry("a.One", "m3", 0),
                new AddonOrder.Entry("a.One", "service", 0)));
        assertEquals(List.of("a.One", "b.Two", "z.Late"), out.stream().map(AddonOrder.Entry::className).toList());
        assertEquals("m3", out.get(0).declaredBy());
    }
}
