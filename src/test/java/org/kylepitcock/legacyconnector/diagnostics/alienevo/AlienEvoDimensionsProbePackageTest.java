package org.kylepitcock.legacyconnector.diagnostics.alienevo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
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
    void mixinConfigurationRegistersOnlyTheMixinAdapter() throws Exception {
        try (var input = AlienEvoDimensionsProbePackageTest.class.getResourceAsStream(
                "/legacyconnector.mixins.json")) {
            assertTrue(input != null, "legacyconnector.mixins.json must be present");
            String json = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject root =
                    com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            var mixins = root.getAsJsonArray("mixins");
            assertTrue(mixins != null);
            boolean adapterRegistered = false;
            boolean utilityRegistered = false;
            for (var entry : mixins) {
                String name = entry.getAsString();
                adapterRegistered |= name.equals("AlienEvoDimensionsProbeMixin");
                utilityRegistered |= name.equals("AlienEvoDimensionsProbe");
            }
            assertTrue(adapterRegistered);
            assertFalse(utilityRegistered);
        }
    }
}
