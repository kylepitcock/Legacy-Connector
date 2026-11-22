package org.kylepitcock.legacyconnector.mixinplugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LegacyMixinPluginTest {
    @Test
    void allowsAlienEvoMixinToTargetPalladiumWhenDependencyIsDeclared() {
        String metadata = """
                [[dependencies.alienevo]]
                modId="forge"
                type="required"
                ordering="NONE"
                versionRange="[43,)"

                [[dependencies.alienevo]]
                modId="palladium"
                type="required"
                ordering="AFTER"
                versionRange="*"
                """;

        Map<String, Set<String>> dependencies =
                LegacyMixinPlugin.parseDeclaredDependencies(metadata, Set.of("alienevo"));

        assertEquals(Set.of("forge", "palladium"), dependencies.get("alienevo"));
        assertTrue(LegacyMixinPlugin.declaresDependency(
                Set.of("alienevo"), Set.of("palladium"), dependencies));
    }

    @Test
    void doesNotAllowCrossModMixinWithoutDeclaredDependency() {
        String metadata = """
                [[dependencies.alienevo]]
                modId="forge"
                type="required"
                ordering="NONE"
                versionRange="[43,)"
                """;

        Map<String, Set<String>> dependencies =
                LegacyMixinPlugin.parseDeclaredDependencies(metadata, Set.of("alienevo"));

        assertFalse(LegacyMixinPlugin.declaresDependency(
                Set.of("alienevo"), Set.of("palladium"), dependencies));
    }

    @Test
    void doesNotGuessPlaceholderDependencyOwnerInMultiModJar() {
        String metadata = """
                [[dependencies.examplemod]]
                modId="palladium"
                type="required"
                ordering="AFTER"
                versionRange="*"
                """;

        Map<String, Set<String>> dependencies =
                LegacyMixinPlugin.parseDeclaredDependencies(metadata, Set.of("alienevo", "fisktag"));

        assertEquals(Map.of(), dependencies);
        assertFalse(LegacyMixinPlugin.declaresDependency(
                Set.of("alienevo", "fisktag"), Set.of("palladium"), dependencies));
    }

    @Test
    void dependencyOfAnotherModInSameJarDoesNotAuthorizeAlienEvoMixin() {
        String metadata = """
                [[dependencies.fisktag]]
                modId="palladium"
                type="required"
                ordering="AFTER"
                versionRange="*"
                """;

        Map<String, Set<String>> dependencies =
                LegacyMixinPlugin.parseDeclaredDependencies(metadata, Set.of("alienevo", "fisktag"));

        assertFalse(LegacyMixinPlugin.declaresDependency(
                Set.of("alienevo"), Set.of("palladium"), dependencies));
        assertTrue(LegacyMixinPlugin.declaresDependency(
                Set.of("fisktag"), Set.of("palladium"), dependencies));
    }
}
