package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
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
                versionRange="[1.20.1,)"
                ordering="NONE"
                side="BOTH"

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
        assertEquals(1, inspected.mods.size());
        LegacyModDecl mod = inspected.mods.get(0);
        assertEquals("alienevo", mod.legacyId);
        assertTrue(mod.parsedDependencies().contains(
                new LegacyModDecl.Dependency("palladium", true, "AFTER", "[1.20.1-4.2.0,)")));
        assertTrue(mod.parsedDependencies().stream()
                .anyMatch(dependency -> dependency.legacyId().equals("forge") && dependency.required()));
        assertTrue(mod.parsedDependencies().stream()
                .anyMatch(dependency -> dependency.legacyId().equals("minecraft") && dependency.required()));
    }
}
