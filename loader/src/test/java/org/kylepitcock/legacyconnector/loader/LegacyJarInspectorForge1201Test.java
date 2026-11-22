package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.kylepitcock.legacyconnector.loader.meta.ModMetadataGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Regression coverage for Forge 1.20.1 TOML dependency metadata. */
class LegacyJarInspectorForge1201Test {

    @Test
    void preservesDependenciesWhenSingleModUsesForgeTemplateOwner(@TempDir Path tempDir) throws Exception {
        Path jarPath = tempDir.resolve("alienevo.jar");
        String modsToml = """
                modLoader="javafml"
                loaderVersion="[43,)"
                license="All rights reserved"

                [[mods]]
                modId="alienevo"
                version="1.5.0-dev.24"
                displayName="AlienEvo"

                [[dependencies.examplemod]]
                modId="forge"
                mandatory=true
                versionRange="[43,)"
                ordering="NONE"
                side="BOTH"

                [[dependencies.examplemod]]
                modId="minecraft"
                mandatory=true
                versionRange="[1.19.2,)"
                ordering="NONE"
                side="BOTH"

                [[dependencies.examplemod]]
                modId="palladium"
                mandatory=true
                versionRange="[1.20.1-4.2.0,)"
                ordering="AFTER"
                side="BOTH"

                [[dependencies.examplemod]]
                modId="terrablender"
                mandatory=true
                versionRange="[3.0.0.0,)"
                ordering="AFTER"
                side="BOTH"
                """;

        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(jarPath))) {
            output.putNextEntry(new ZipEntry("META-INF/mods.toml"));
            output.write(modsToml.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        LegacyJar inspected;
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            inspected = LegacyJarInspector.inspectForge1201(zip, jarPath);
        }

        assertNotNull(inspected);
        assertEquals(1, inspected.mods.size());
        LegacyModDecl mod = inspected.mods.get(0);
        assertEquals("alienevo", mod.legacyId);
        assertTrue(mod.parsedDependencies().contains(
                new LegacyModDecl.Dependency("palladium", true, "AFTER", "[1.20.1-4.2.0,)")));
        assertTrue(mod.parsedDependencies().stream()
                .anyMatch(dependency -> dependency.legacyId().equals("forge") && dependency.required()));
        assertTrue(mod.parsedDependencies().stream()
                .anyMatch(dependency -> dependency.legacyId().equals("minecraft") && dependency.required()));
        assertTrue(mod.parsedDependencies().contains(
                new LegacyModDecl.Dependency("terrablender", true, "AFTER", "[3.0.0.0,)")));

        ModMetadataGenerator.setLoadedMods(List.of("alienevo", "palladium", "terrablender"));
        try {
            String generated = new String(ModMetadataGenerator.modsToml(inspected), StandardCharsets.UTF_8);
            assertTrue(generated.contains("[[dependencies.alienevo]]"));
            assertTrue(generated.contains("modId=\"palladium\"\ntype=\"required\"\nversionRange=\"*\"\nordering=\"AFTER\""));
        } finally {
            ModMetadataGenerator.setLoadedMods(List.of());
        }
    }

    @Test
    void doesNotGuessPlaceholderOwnerForMultiModJar(@TempDir Path tempDir) throws Exception {
        Path jarPath = tempDir.resolve("multi-mod.jar");
        String modsToml = """
                modLoader="javafml"
                loaderVersion="[43,)"

                [[mods]]
                modId="firstmod"
                version="1.0"
                displayName="First"

                [[mods]]
                modId="secondmod"
                version="1.0"
                displayName="Second"

                [[dependencies.examplemod]]
                modId="palladium"
                mandatory=true
                versionRange="[1.20.1-4.2.0,)"
                ordering="AFTER"
                side="BOTH"
                """;

        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(jarPath))) {
            output.putNextEntry(new ZipEntry("META-INF/mods.toml"));
            output.write(modsToml.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        LegacyJar inspected;
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            inspected = LegacyJarInspector.inspectForge1201(zip, jarPath);
        }

        assertNotNull(inspected);
        assertEquals(2, inspected.mods.size());
        assertTrue(inspected.mods.stream().allMatch(mod -> mod.dependencies.isEmpty()),
                "An ambiguous placeholder owner must not assign the dependency to either mod");
    }
}
