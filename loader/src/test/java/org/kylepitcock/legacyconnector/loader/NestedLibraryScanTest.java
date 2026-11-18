package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A library a modern mod nests anywhere under META-INF (C2ME: META-INF/jars/caffeine.jar) counts as a provider, so a legacy copy of it is relocated. */
class NestedLibraryScanTest {
    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (var e : files.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                out.write(e.getValue());
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    @Test
    void nestedLibraryFolders() {
        assertTrue(BundledLibraries.isNestedLibraryJar("META-INF/jarjar/a.jar"));
        assertTrue(BundledLibraries.isNestedLibraryJar("META-INF/jars/caffeine-3.2.1.jar"));
        assertFalse(BundledLibraries.isNestedLibraryJar("META-INF/a.jar"));
        assertFalse(BundledLibraries.isNestedLibraryJar("lib/a.jar"));
    }

    @Test
    void providerInMetaInfJars(@TempDir Path dir) throws IOException {
        byte[] inner = zip(Map.of("com/github/benmanes/caffeine/cache/Caffeine.class", new byte[] {1}));
        Files.write(dir.resolve("c2me.jar"), zip(Map.of("META-INF/jars/caffeine-3.2.1.jar", inner)));
        Map<String, String> providers = BundledLibraries.scanProviders(List.of(dir), Set.of());
        assertEquals("c2me.jar (nested caffeine-3.2.1.jar)", providers.get("com/github/benmanes/caffeine/cache"));
    }
}
