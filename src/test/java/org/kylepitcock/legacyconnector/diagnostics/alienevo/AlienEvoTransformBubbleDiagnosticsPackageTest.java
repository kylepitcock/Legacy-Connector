package org.kylepitcock.legacyconnector.diagnostics.alienevo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AlienEvoTransformBubbleDiagnosticsPackageTest {
    @Test
    void optionalClientMixinIsRegisteredAndRuntimeProbeLivesOutsideMixinPackage() throws IOException {
        assertNotNull(getClass().getResource(
                "/org/kylepitcock/legacyconnector/mixin/AlienEvoTransformBubbleDiagnosticsMixin.class"));
        assertNotNull(getClass().getResource(
                "/org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoTransformBubbleDiagnostics1201Runtime.class"));
        try (InputStream input = getClass().getResourceAsStream("/legacyconnector.mixins.json")) {
            assertNotNull(input, "legacyconnector.mixins.json must be on the runtime classpath");
            String config = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            com.google.gson.JsonObject root =
                    com.google.gson.JsonParser.parseString(config).getAsJsonObject();
            var clientMixins = root.getAsJsonArray("client");
            assertNotNull(clientMixins);
            boolean registered = false;
            for (var entry : clientMixins) {
                registered |= entry.getAsString().equals("AlienEvoTransformBubbleDiagnosticsMixin");
            }
            assertTrue(registered, "the optional AlienEvo diagnostic Mixin must be registered client-side");
        }
    }
}
