package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class LegacyModReportNotesTest {

    @Test
    void reportsForge1201MixinsAsMigratedButStillRuntimeSensitive() {
        String note = LegacyModReportNotes.mixinStatus(
                LegacyVersion.V1_20_1,
                "palladium-common.mixins.json,palladium.mixins.json");

        assertTrue(note.startsWith("NOTE: ships mixins"));
        assertTrue(note.contains("rewritten for Minecraft 1.21.1"));
        assertTrue(note.contains("registered with NeoForge"));
        assertTrue(note.contains("Individual injections may still fail"));
        assertTrue(note.contains("latest.log"));
        assertFalse(note.contains("not applied"));
    }

    @Test
    void explainsTheNarrowTranslatedClassMixinSupportFor1122() {
        String note = LegacyModReportNotes.mixinStatus(
                LegacyVersion.V1_12_2,
                "example.mixins.json");

        assertTrue(note.startsWith("WARNING: ships mixins"));
        assertTrue(note.contains("only mixins targeting translated mod classes are applied"));
    }

    @Test
    void retainsTheUnsupportedLegacyMixinWarningForOlderVersions() {
        String note = LegacyModReportNotes.mixinStatus(
                LegacyVersion.V1_7_10,
                "example.mixins.json");

        assertTrue(note.startsWith("WARNING: ships mixins"));
        assertTrue(note.endsWith("targeting legacy Minecraft; not applied."));
    }
}
