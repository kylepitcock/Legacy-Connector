package org.kylepitcock.legacyconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.core.LegacyMods;

class LegacyVersionTest {
    @Test
    void everyVersionIsFoundById() {
        for (LegacyVersion v : LegacyVersion.values()) assertEquals(v, LegacyVersion.byId(v.id()).orElseThrow());
    }

    @Test
    void familyMatchesThePlatformFamilyOfTheCore() {
        // the API view and the core's platform lookup must agree which versions share the 1.7.10 shim
        for (LegacyVersion v : LegacyVersion.values()) assertEquals(LegacyMods.family(v.id()), v.family().id(), v.id());
    }

    @Test
    void numericIdVersions() {
        assertTrue(LegacyVersion.V1_4_7.usesNumericIds());
        assertTrue(LegacyVersion.V1_5_2.usesNumericIds());
        assertTrue(LegacyVersion.V1_6_4.usesNumericIds());
        assertFalse(LegacyVersion.V1_7_10.usesNumericIds());
        assertFalse(LegacyVersion.V1_12_2.usesNumericIds());
        assertFalse(LegacyVersion.V1_20_1.usesNumericIds());
    }

    @Test
    void onlyThe1201LayerIsInPlace() {
        for (LegacyVersion v : LegacyVersion.values()) assertEquals(v == LegacyVersion.V1_20_1, v.isInPlace(), v.id());
    }

    @Test
    void existingConstantsKeepTheirNamesAndIds() {
        assertEquals("1.7.10", LegacyVersion.V1_7_10.id());
        assertEquals("1.12.2", LegacyVersion.V1_12_2.id());
        assertEquals(LegacyVersion.V1_7_10, LegacyVersion.V1_7_10.family());
        assertEquals(LegacyVersion.V1_12_2, LegacyVersion.V1_12_2.family());
        assertEquals(LegacyVersion.V1_7_10, LegacyVersion.V1_4_7.family());
    }
}
