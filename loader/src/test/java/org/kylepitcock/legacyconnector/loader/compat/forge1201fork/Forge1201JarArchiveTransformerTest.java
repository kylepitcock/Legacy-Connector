package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201JarArchiveTransformerTest {
    @TempDir
    Path tempDir;

    private static final String SOURCE_TOML = """
            modLoader="javafml"
            loaderVersion="[47,)"
            license="All Rights Reserved"

            [[mods]]
            modId="symbiote"
            version="1.1.3"
            displayName="Symbiote"
            description="Synthetic test fixture"

            [[dependencies.symbiote]]
            modId="minecraft"
            mandatory=true
            versionRange="[1.20.1,1.21)"
            ordering="NONE"
            side="BOTH"

            [[dependencies.symbiote]]
            modId="forge"
            mandatory=true
            versionRange="[47,)"
            ordering="NONE"
            side="BOTH"

            [[dependencies.symbiote]]
            modId="curios"
            mandatory=false
            versionRange="[5.11,)"
            ordering="AFTER"
            side="BOTH"
            """;

    @Test
    void createsDerivedJarAndPreservesOriginal() throws Exception {
        Path original = createSourceJar();
        Path translated = tempDir.resolve("translated").resolve("symbiote-metadata-only.jar");
        String before = sha256(original);

        Forge1201JarArchiveTransformer.Result result = Forge1201JarArchiveTransformer.transform(
                original, translated, "[1,)", "[21.1.258,)", "[1.21.1,1.22)");

        assertTrue(Files.isRegularFile(translated));
        assertEquals(before, sha256(original));
        assertEquals(before, result.inputSha256());
        assertEquals(sha256(translated), result.outputSha256());
        assertNotEquals(result.inputSha256(), result.outputSha256());
        assertEquals(3, result.removedSignatureEntries());

        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(translated.toFile())) {
            assertTrue(zip.getEntry("META-INF/mods.toml") == null);
            assertTrue(zip.getEntry("META-INF/neoforge.mods.toml") != null);
            assertTrue(zip.getEntry("META-INF/MANIFEST.MF") != null);
            assertTrue(zip.getEntry("assets/symbiote/test.txt") != null);

            assertTrue(zip.getEntry("META-INF/TEST.SF") == null);
            assertTrue(zip.getEntry("META-INF/CERT.RSA") == null);
            assertTrue(zip.getEntry("META-INF/SIG-FOO") == null);
            assertTrue(zip.getEntry("META-INF/nested/keep.sf") != null);

            String metadata = new String(
                    zip.getInputStream(zip.getEntry("META-INF/neoforge.mods.toml")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertTrue(metadata.contains("loaderVersion=\"[1,)\""));
            assertTrue(metadata.contains("modId=\"neoforge\""));
            assertTrue(metadata.contains("type=\"required\""));
            assertTrue(metadata.contains("type=\"optional\""));
            assertFalse(metadata.contains("modId=\"forge\""));
            assertFalse(metadata.contains("mandatory="));
            assertTrue(metadata.contains("license=\"All Rights Reserved\""));
        }
    }

    @Test
    void refusesToOverwriteOriginalJar() throws Exception {
        Path original = createSourceJar();
        String before = sha256(original);

        assertThrows(IllegalArgumentException.class, () -> Forge1201JarArchiveTransformer.transform(
                original, original, "[1,)", "[21.1.258,)", "[1.21.1,1.22)"));

        assertEquals(before, sha256(original));
    }

    @Test
    void refusesAnUnrecognizedJarWithoutCreatingOutput() throws Exception {
        Path input = tempDir.resolve("not-a-forge-mod.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(input))) {
            put(out, "readme.txt", "not a mod");
        }
        Path output = tempDir.resolve("bad-output.jar");

        assertThrows(IllegalArgumentException.class, () -> Forge1201JarArchiveTransformer.transform(
                input, output, "[1,)", "[21.1.258,)", "[1.21.1,1.22)"));

        assertFalse(Files.exists(output));
    }

    private Path createSourceJar() throws IOException {
        Path jar = tempDir.resolve("source-symbiote.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            put(out, "META-INF/MANIFEST.MF", """
                    Manifest-Version: 1.0
                    Implementation-Version: 1.1.3

                    """);
            put(out, "META-INF/mods.toml", SOURCE_TOML);
            put(out, "META-INF/TEST.SF", "signature-file");
            put(out, "META-INF/CERT.RSA", "signature-block");
            put(out, "META-INF/SIG-FOO", "signature-data");
            put(out, "META-INF/nested/keep.sf", "ordinary nested resource");
            put(out, "assets/symbiote/test.txt", "keep this resource");
            put(out, "com/scout/symbiote/SymbioteMod.class", new byte[] {0, 1, 2, 3});
        }
        return jar;
    }

    private static void put(ZipOutputStream out, String name, String text) throws IOException {
        put(out, name, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void put(ZipOutputStream out, String name, byte[] content) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(content);
        out.closeEntry();
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
