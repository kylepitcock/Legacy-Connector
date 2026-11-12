package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

/** Regression coverage for nested Forge 1.20.1 libraries and host-provided duplicate removal. */
class Forge1201NestedJarTest {

    @Test
    void onlyHostProvidedMixinExtrasLibrariesAreDropped() {
        assertTrue(Forge1201Transformer.providedByHost("META-INF/jars/mixinextras-forge-0.2.0.jar"));
        assertTrue(Forge1201Transformer.providedByHost("META-INF/jarjar/MixinExtras-Forge-0.5.0.jar"));
        assertFalse(Forge1201Transformer.providedByHost("META-INF/jars/player-animation-lib-forge-1.0.2-rc1+1.20.jar"));
        assertFalse(Forge1201Transformer.providedByHost("META-INF/jars/palladiumcore-forge-1.0.1.jar"));
    }

    @Test
    void jarJarMetadataDropsOnlyHostProvidedLibraries() {
        String source = """
                {"jars":[
                  {"identifier":{"group":"io.github.llamalad7","artifact":"mixinextras-forge","version":"0.2.0"},"path":"META-INF/jars/mixinextras-forge-0.2.0.jar"},
                  {"identifier":{"group":"net.threetag","artifact":"palladiumcore-forge"},"version":{"range":"[1.0.1+1.20.1,)","artifactVersion":"1.0.1+1.20.1"},"path":"META-INF/jars/palladiumcore-forge-1.0.1+1.20.1-forge.jar"},
                  {"identifier":{"group":"dev.kosmx.player-anim","artifact":"player-animation-lib-forge"},"version":{"range":"[1.0.2-rc1+1.20,)","artifactVersion":"1.0.2-rc1+1.20"},"path":"META-INF/jars/player-animation-lib-forge-1.0.2-rc1+1.20.jar"}
                ]}
                """;

        JsonObject migrated = JsonParser.parseString(Forge1201Transformer.jarJarMetadata(source)).getAsJsonObject();
        JsonArray jars = migrated.getAsJsonArray("jars");

        assertEquals(2, jars.size());
        assertTrue(jars.toString().contains("palladiumcore-forge-1.0.1+1.20.1-forge.jar"));
        assertTrue(jars.toString().contains("player-animation-lib-forge-1.0.2-rc1+1.20.jar"));
        JsonObject playerAnimator = jars.get(1).getAsJsonObject();
        assertEquals("[1.0.2-rc1+1.20,)",
                playerAnimator.getAsJsonObject("version").get("range").getAsString());
        assertFalse(jars.toString().contains("mixinextras-forge-0.2.0.jar"));
    }

    @Test
    void malformedJarJarMetadataIsPreservedForDiagnostics() {
        String malformed = "{not-json";
        assertEquals(malformed, Forge1201Transformer.jarJarMetadata(malformed));
    }
}
