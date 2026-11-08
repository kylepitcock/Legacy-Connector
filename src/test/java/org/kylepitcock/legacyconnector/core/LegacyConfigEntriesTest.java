package org.kylepitcock.legacyconnector.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LegacyConfigEntriesTest {
    @Test
    void templateEntriesAreListedWithDescriptions() {
        var entries = LegacyConfig.entries();
        var autoSort = entries.stream().filter(e -> e.key().equals("legacy.autoSortMods")).findFirst().orElseThrow();
        assertTrue(autoSort.isBoolean());
        assertEquals("true", autoSort.defaultValue());
        assertTrue(autoSort.description().contains("mods/"));
        assertTrue(entries.stream().anyMatch(e -> e.key().equals("experimental.forge1201.softlink")));
        assertTrue(entries.stream().anyMatch(e -> e.key().equals("energy.fePerEu") && !e.isBoolean()));
        assertTrue(entries.stream().noneMatch(e -> e.key().startsWith("#") || e.key().equals("mods.alias.oldid")));
    }
}
