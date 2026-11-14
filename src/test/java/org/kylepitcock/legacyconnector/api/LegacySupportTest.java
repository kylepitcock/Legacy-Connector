package org.kylepitcock.legacyconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.api.LegacySupport.Feature;
import org.kylepitcock.legacyconnector.api.LegacySupport.Level;

class LegacySupportTest {
    private static final String BEGIN = "<!-- support-matrix:begin (generated from LegacySupport; run the test with API_DOCS_UPDATE=1 to rewrite) -->";
    private static final String END = "<!-- support-matrix:end -->";

    @Test
    void everyVersionAndFeatureIsAnswered() {
        assertEquals(LegacyVersion.values().length, LegacySupport.versions().length);
        for (LegacyVersion v : LegacyVersion.values()) {
            for (Feature f : Feature.values()) assertNotNull(LegacySupport.of(f, v).level(), f + " " + v);
        }
    }

    @Test
    void betaHasNoEnergyFluidsOrItemCapabilities() {
        for (Feature f : new Feature[] {Feature.ENERGY_RF, Feature.ENERGY_EU, Feature.ENERGY_MJ, Feature.FLUID_LOOKUP, Feature.BUCKET_LOOKUP,
                Feature.FLUID_HANDLER_TILES, Feature.ITEM_CAPABILITIES}) {
            assertEquals(Level.NOT_APPLICABLE, LegacySupport.of(f, LegacyVersion.VB1_7_3).level(), f.name());
            assertFalse(LegacySupport.of(f, LegacyVersion.VB1_7_3).available());
        }
        assertTrue(LegacySupport.of(Feature.ITEM_HANDLER_TILES, LegacyVersion.VB1_7_3).available());
    }

    @Test
    void stateConversionExistsOnlyFor1122AndInPlace() {
        for (LegacyVersion v : LegacyVersion.values()) {
            boolean expected = v == LegacyVersion.V1_12_2 || v == LegacyVersion.V1_20_1;
            assertEquals(expected, LegacySupport.of(Feature.BLOCK_STATE_CONVERSION, v).available(), v.id());
        }
    }

    @Test
    void numericIdsMatchTheVersionFlags() {
        for (LegacyVersion v : LegacyVersion.values()) {
            assertEquals(v.usesNumericIds() || v.isBeta(), LegacySupport.of(Feature.NUMERIC_IDS, v).available(), v.id());
        }
    }

    @Test
    void documentationTableIsGeneratedFromTheMatrix() throws Exception {
        Path doc = Path.of("docs/wiki/Addon-API.md");
        String text = Files.readString(doc, StandardCharsets.UTF_8).replace("\r\n", "\n");
        int begin = text.indexOf(BEGIN);
        int end = text.indexOf(END);
        assertTrue(begin >= 0 && end > begin, "docs/wiki/Addon-API.md has the support-matrix markers");
        String expected = BEGIN + "\n\n" + LegacySupport.markdownTable() + "\n" + END;
        String actual = text.substring(begin, end + END.length());
        if ((Boolean.getBoolean("api.docs.update") || System.getenv("API_DOCS_UPDATE") != null) && !expected.equals(actual)) {
            Files.writeString(doc, text.substring(0, begin) + expected + text.substring(end + END.length()), StandardCharsets.UTF_8);
            return;
        }
        assertEquals(expected, actual, "the support matrix in the documentation is out of date: run the test with API_DOCS_UPDATE=1");
    }
}
