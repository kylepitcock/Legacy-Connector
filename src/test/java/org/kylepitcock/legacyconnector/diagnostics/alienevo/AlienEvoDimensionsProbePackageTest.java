package org.kylepitcock.legacyconnector.diagnostics.alienevo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

class AlienEvoDimensionsProbePackageTest {
    @Test
    void diagnosticProbeLivesOutsideTheMixinPackageAndExposesItsHookMethods() throws Exception {
        assertEquals("org.kylepitcock.legacyconnector.diagnostics.alienevo",
                AlienEvoDimensionsProbe.class.getPackageName());
        assertTrue(Modifier.isPublic(AlienEvoDimensionsProbe.class.getModifiers()));
        assertTrue(Modifier.isPublic(AlienEvoDimensionsProbe.class
                .getDeclaredMethod("record", Player.class, String.class).getModifiers()));
        assertTrue(Modifier.isPublic(AlienEvoDimensionsProbe.class
                .getDeclaredMethod("sample", Player.class).getModifiers()));
    }

    @Test
    void utilityBytecodeIsOutsideTheMixinOwnedPackage() {
        assertNotNull(getClass().getResource(
                "/org/kylepitcock/legacyconnector/diagnostics/alienevo/AlienEvoDimensionsProbe.class"));
        assertNotNull(getClass().getResource(
                "/org/kylepitcock/legacyconnector/mixin/AlienEvoDimensionsProbeMixin.class"));
        assertNotNull(getClass().getResource(
                "/org/kylepitcock/legacyconnector/mixin/AlienEvoLivingDimensions1201Mixin.class"));
        assertNull(getClass().getResource(
                "/org/kylepitcock/legacyconnector/mixin/AlienEvoDimensionsProbe.class"),
                "A non-Mixin helper must not be emitted in the configured Mixin package");
    }

    @Test
    void mixinConfigurationRegistersTheMixinAdapterButNotTheHelper() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/legacyconnector.mixins.json")) {
            assertNotNull(input, "legacyconnector.mixins.json must be on the runtime classpath");
            String config = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(config.contains("\"package\": \"org.kylepitcock.legacyconnector.mixin\""));

            com.google.gson.JsonObject root =
                    com.google.gson.JsonParser.parseString(config).getAsJsonObject();
            var mixins = root.getAsJsonArray("mixins");
            assertNotNull(mixins);

            boolean probeAdapterRegistered = false;
            boolean sizeAdapterRegistered = false;
            boolean utilityRegistered = false;
            for (var entry : mixins) {
                String name = entry.getAsString();
                probeAdapterRegistered |= name.equals("AlienEvoDimensionsProbeMixin");
                sizeAdapterRegistered |= name.equals("AlienEvoLivingDimensions1201Mixin");
                utilityRegistered |= name.equals("AlienEvoDimensionsProbe");
            }

            assertTrue(probeAdapterRegistered, "The dimensions diagnostic Mixin adapter must be registered");
            assertTrue(sizeAdapterRegistered, "The 1.21.1 AlienEvo size adapter must be registered");
            assertFalse(utilityRegistered, "The plain diagnostics utility must not be registered as a Mixin");
            assertTrue(config.contains("\"AlienEvoDimensionsProbeMixin\""));
            assertTrue(config.contains("\"AlienEvoLivingDimensions1201Mixin\""));
            assertFalse(config.contains("\"AlienEvoDimensionsProbe\","));
        }
    }
}
