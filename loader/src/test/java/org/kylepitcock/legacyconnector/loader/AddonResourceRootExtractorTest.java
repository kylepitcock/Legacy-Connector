package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AddonResourceRootExtractorTest {
    @TempDir Path temporaryDirectory;

    @AfterEach
    void clearRootProperty() {
        System.clearProperty(AddonResourceRootExtractor.ROOTS_PROPERTY);
    }

    @Test
    void extractsMarkerBearingAddonRootsAndIgnoresUnrelatedTranslatedJars() throws Exception {
        Path withMarker = temporaryDirectory.resolve("alienevo-translated.jar");
        writeJar(withMarker, true, "addon/alienevo/kubejs_scripts/quick_change.js", "quick-change");
        Path unrelated = temporaryDirectory.resolve("unrelated-translated.jar");
        writeJar(unrelated, false, "assets/unrelated/ignored.txt", "ignore-me");

        List<Path> roots = AddonResourceRootExtractor.extract(
                temporaryDirectory.resolve("cache"), List.of(unrelated, withMarker));

        assertEquals(1, roots.size());
        Path addonRoot = roots.get(0);
        assertTrue(Files.isRegularFile(addonRoot.resolve(".mcassetsroot")));
        assertEquals("quick-change",
                Files.readString(addonRoot.resolve("alienevo/kubejs_scripts/quick_change.js")));
        assertFalse(Files.exists(addonRoot.resolve("unrelated/ignored.txt")));
        assertEquals(addonRoot.toString(), System.getProperty(AddonResourceRootExtractor.ROOTS_PROPERTY));
    }

    @Test
    void synthesizesMarkerForAddonPayloadThatOmitsTheClasspathMarker() throws Exception {
        Path markerless = temporaryDirectory.resolve("markerless-addon-translated.jar");
        writeJar(markerless, false, "addon/alienevo/kubejs_scripts/quick_change.js", "quick-change");

        List<Path> roots = AddonResourceRootExtractor.extract(
                temporaryDirectory.resolve("cache"), List.of(markerless));

        assertEquals(1, roots.size());
        Path addonRoot = roots.get(0);
        assertTrue(Files.isRegularFile(addonRoot.resolve(".mcassetsroot")));
        assertEquals(0L, Files.size(addonRoot.resolve(".mcassetsroot")));
        assertEquals("quick-change",
                Files.readString(addonRoot.resolve("alienevo/kubejs_scripts/quick_change.js")));
        assertEquals(addonRoot.toString(), System.getProperty(AddonResourceRootExtractor.ROOTS_PROPERTY));
    }

    @Test
    void doesNotRegisterAnEmptyRootWhenNoAddonPayloadExists() throws Exception {
        Path unrelated = temporaryDirectory.resolve("no-addon-translated.jar");
        writeJar(unrelated, false, "assets/alienevo/ignored.txt", "ignore-me");

        List<Path> roots = AddonResourceRootExtractor.extract(
                temporaryDirectory.resolve("cache"), List.of(unrelated));

        assertTrue(roots.isEmpty());
        assertFalse(System.getProperties().containsKey(AddonResourceRootExtractor.ROOTS_PROPERTY));
    }

    @Test
    void ignoresArchiveEntriesThatAttemptToEscapeTheAddonRoot() throws Exception {
        Path archive = temporaryDirectory.resolve("malformed.jar");
        writeJar(archive, true, "addon/../../escape.txt", "must-not-escape");

        List<Path> roots = AddonResourceRootExtractor.extract(
                temporaryDirectory.resolve("cache"), List.of(archive));

        assertEquals(1, roots.size());
        assertFalse(Files.exists(temporaryDirectory.resolve("escape.txt")));
    }

    private static void writeJar(Path path, boolean marker, String resource, String content) throws Exception {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path))) {
            if (marker) {
                out.putNextEntry(new ZipEntry("addon/.mcassetsroot"));
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry(resource));
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }
}
