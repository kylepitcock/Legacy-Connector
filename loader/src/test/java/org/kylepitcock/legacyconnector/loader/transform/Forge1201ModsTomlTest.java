package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Forge1201ModsTomlTest {
    @Test
    void associatesAlienEvoDependencyTablesWithItsDeclaredModId() {
        String input = """
                modLoader="javafml"
                loaderVersion="[43,)"
                [[mods]]
                modId="alienevo"
                version="1.1.2"
                [[dependencies.examplemod]]
                modId="forge"
                mandatory=true
                versionRange="[43,)"
                [[dependencies.examplemod]]
                modId="palladium"
                mandatory=true
                versionRange="[1.20.1-4.2.0,)"
                """;
        String migrated = Forge1201Transformer.modsToml(input);
        assertTrue(migrated.contains("[[dependencies.alienevo]]"), migrated);
        assertFalse(migrated.contains("[[dependencies.examplemod]]"), migrated);
        assertTrue(migrated.contains("modId=\"neoforge\""), migrated);
        assertTrue(migrated.contains("modId=\"palladium\""), migrated);
    }

    @Test
    void retargetsTheInlineDependencyForm() {
        String input = """
                modLoader="javafml"
                [[mods]]
                modId="repurposed_structures"
                version="7.1.19"

                [dependencies]
                "repurposed_structures" = [
                    { modId = "forge", mandatory = true, versionRange = "[46.0.1,)", ordering = "NONE", side = "BOTH" },
                    { modId = "minecraft", mandatory = true, versionRange = "[1.20.1,1.21)", ordering = "NONE", side = "BOTH" },
                ]
                """;
        String migrated = Forge1201Transformer.modsToml(input);
        assertTrue(migrated.contains("[[dependencies.repurposed_structures]]"), migrated);
        assertTrue(migrated.contains("modId=\"neoforge\"") || migrated.contains("modId = \"neoforge\""), migrated);
        assertFalse(migrated.contains("[1.20.1,1.21)"), migrated);
        assertFalse(migrated.contains("modId = \"forge\""), migrated);
    }

    @Test
    void leavesMultiModDependencyOwnersUntouched() {
        String input = """
                [[mods]]
                modId="first"
                [[mods]]
                modId="second"
                [[dependencies.examplemod]]
                modId="optional"
                mandatory=false
                versionRange="[1,)"
                """;
        String migrated = Forge1201Transformer.modsToml(input);
        assertTrue(migrated.contains("[[dependencies.examplemod]]"), migrated);
        assertFalse(migrated.contains("[[dependencies.first]]"), migrated);
    }
}
