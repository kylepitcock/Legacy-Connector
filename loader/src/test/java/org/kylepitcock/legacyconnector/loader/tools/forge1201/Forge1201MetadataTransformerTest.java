package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class Forge1201MetadataTransformerTest {
    private static final String FORGE_DEPENDENCY = """
            [[dependencies.symbiote]]
            modId="forge"
            mandatory=true
            versionRange="[47,)"
            ordering="NONE"
            side="BOTH"
            """;

    private static final String MINECRAFT_DEPENDENCY = """
            [[dependencies.symbiote]]
            modId="minecraft"
            mandatory=true
            versionRange="[1.20.1,1.21)"
            ordering="NONE"
            side="BOTH"
            """;

    private static final String OPTIONAL_CURIOS_DEPENDENCY = """
            [[dependencies.symbiote]]
            modId="curios"
            mandatory=false
            versionRange="[5.11,)"
            ordering="AFTER"
            side="BOTH"
            """;

    private static final String SOURCE_METADATA = """
            modLoader="javafml"
            loaderVersion="[47,)"
            license="All Rights Reserved"

            [[mods]]
            modId="symbiote"
            version="1.1.3"
            displayName="Symbiote"
            authors="kitigawa"
            description="A # remains part of this quoted description"

            %s
            %s
            %s
            """.formatted(FORGE_DEPENDENCY, MINECRAFT_DEPENDENCY, OPTIONAL_CURIOS_DEPENDENCY);

    @Test
    void rewritesLoaderAndMandatoryPlatformDependencies() {
        String result = transform(SOURCE_METADATA);

        assertTrue(result.contains("modLoader=\"javafml\""));
        assertTrue(result.contains("loaderVersion=\"[1,)\""));
        assertTrue(result.contains("modId=\"neoforge\""));
        assertTrue(result.contains("versionRange=\"[21.1.258,)\""));
        assertTrue(result.contains("modId=\"minecraft\""));
        assertTrue(result.contains("versionRange=\"[1.21.1,1.22)\""));
        assertTrue(result.contains("type=\"required\""));
        assertFalse(result.contains("mandatory="));
        assertFalse(result.contains("modId=\"forge\""));
        assertFalse(result.contains("[47,)"));
        assertFalse(result.contains("[1.20.1,1.21)"));
    }

    @Test
    void preservesModMetadataCommentsAndOptionalDependencies() {
        String result = transform(SOURCE_METADATA);

        assertTrue(result.contains("license=\"All Rights Reserved\""));
        assertTrue(result.contains("modId=\"symbiote\""));
        assertTrue(result.contains("version=\"1.1.3\""));
        assertTrue(result.contains("displayName=\"Symbiote\""));
        assertTrue(result.contains("authors=\"kitigawa\""));
        assertTrue(result.contains("description=\"A # remains part of this quoted description\""));
        assertTrue(result.contains("modId=\"curios\""));
        assertTrue(result.contains("type=\"optional\""));
        assertFalse(result.contains("mandatory="));
        assertTrue(result.contains("versionRange=\"[5.11,)\""));
        assertTrue(result.contains("ordering=\"AFTER\""));
        assertTrue(result.contains("side=\"BOTH\""));
    }

    @Test
    void leavesAnOptionalForgeDependencyUntouchedAndRejectsItAsPlatformRequirement() {
        String optionalForge = FORGE_DEPENDENCY.replace("mandatory=true", "mandatory=false");
        String source = """
                modLoader="javafml"
                loaderVersion="[47,)"

                [[mods]]
                modId="symbiote"
                version="1.1.3"

                %s
                %s
                """.formatted(optionalForge, MINECRAFT_DEPENDENCY);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> transform(source));

        assertTrue(error.getMessage().contains("no mandatory Forge dependency"));
    }

    @Test
    void rejectsMetadataThatDoesNotUseJavaFml() {
        String source = SOURCE_METADATA.replace("modLoader=\"javafml\"", "modLoader=\"lowcode\"");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> transform(source));

        assertTrue(error.getMessage().contains("modLoader=javafml"));
    }

    @Test
    void rejectsMetadataWithoutLoaderVersionOrMinecraftDependency() {
        String withoutLoaderVersion = SOURCE_METADATA.replace("loaderVersion=\"[47,)\"", "");
        assertThrows(IllegalArgumentException.class, () -> transform(withoutLoaderVersion));

        String withoutMinecraft = SOURCE_METADATA.replace(MINECRAFT_DEPENDENCY, "");
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> transform(withoutMinecraft));

        assertTrue(error.getMessage().contains("no mandatory Minecraft dependency"));
    }

    @Test
    void rejectsMissingMandatoryForgeDependency() {
        String withoutForge = SOURCE_METADATA.replace(FORGE_DEPENDENCY, "");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> transform(withoutForge));

        assertTrue(error.getMessage().contains("no mandatory Forge dependency"));
    }

    @Test
    void rejectsInvalidReplacementRanges() {
        assertThrows(IllegalArgumentException.class, () -> Forge1201MetadataTransformer.transformToml(
                SOURCE_METADATA, "[1,)", "[21.1.258,)", "[1.21.1,1.22)\nmalicious=true"));
    }

    private static String transform(String source) {
        return Forge1201MetadataTransformer.transformToml(
                source,
                "[1,)",
                "[21.1.258,)",
                "[1.21.1,1.22)");
    }
}
