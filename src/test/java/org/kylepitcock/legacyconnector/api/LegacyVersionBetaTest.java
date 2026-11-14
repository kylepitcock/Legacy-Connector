package org.kylepitcock.legacyconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LegacyVersionBetaTest {
    @Test
    void betaIsItsOwnFamilyAndNotTheNumericIdFamily() {
        LegacyVersion b = LegacyVersion.byId("b1.7.3").orElseThrow();
        assertEquals(LegacyVersion.VB1_7_3, b);
        assertTrue(b.isBeta());
        assertEquals(b, b.family());
        assertFalse(b.usesNumericIds());
        assertFalse(b.isInPlace());
        for (LegacyVersion v : LegacyVersion.values()) assertEquals(v == b, v.isBeta(), v.id());
    }

    @Test
    void versionOfAClassWithoutMods() {
        // no legacy mod is loaded in a unit test: classes of the shim are told by their package, anything else is unknown
        assertEquals(java.util.Optional.empty(), LegacyApi.versionOf(String.class));
        assertEquals(java.util.Optional.empty(), LegacyApi.versionOfNamespace("minecraft"));
        assertEquals(java.util.Optional.empty(), LegacyApi.versionOf(null));
        assertEquals(java.util.List.of(), LegacyApi.failedMods());
    }
}
