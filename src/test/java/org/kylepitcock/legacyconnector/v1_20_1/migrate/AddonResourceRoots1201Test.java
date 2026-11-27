package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AddonResourceRoots1201Test {
    @TempDir Path temporaryDirectory;

    @AfterEach
    void clearRootProperty() {
        System.clearProperty(AddonResourceRoots1201.ROOTS_PROPERTY);
    }

    @Test
    void appendsExtractedAddonMarkerToVanillaClasspathDiscovery() throws Exception {
        Path addonRoot = temporaryDirectory.resolve("extracted-addon");
        Files.createDirectories(addonRoot);
        Path marker = Files.createFile(addonRoot.resolve(".mcassetsroot"));
        System.setProperty(AddonResourceRoots1201.ROOTS_PROPERTY, addonRoot.toString());

        Enumeration<URL> result = AddonResourceRoots1201.appendAddonRootMarkers(
                "/addon/.mcassetsroot", Collections.emptyEnumeration());

        assertTrue(result.hasMoreElements());
        assertEquals(marker.toUri().toURL(), result.nextElement());
        assertFalse(result.hasMoreElements());
    }

    @Test
    void preservesExistingClasspathMarkersAndLeavesOtherResourcesUntouched() throws Exception {
        URL existingUrl = temporaryDirectory.resolve("existing/.mcassetsroot").toUri().toURL();
        Enumeration<URL> existing = Collections.enumeration(List.of(existingUrl));
        assertSame(existing, AddonResourceRoots1201.appendAddonRootMarkers("assets/.mcassetsroot", existing));

        Path addonRoot = temporaryDirectory.resolve("addon-root");
        Files.createDirectories(addonRoot);
        URL addonMarker = Files.createFile(addonRoot.resolve(".mcassetsroot")).toUri().toURL();
        System.setProperty(AddonResourceRoots1201.ROOTS_PROPERTY, addonRoot.toString());

        Enumeration<URL> result = AddonResourceRoots1201.appendAddonRootMarkers(
                "addon/.mcassetsroot", Collections.enumeration(List.of(existingUrl)));
        assertEquals(existingUrl, result.nextElement());
        assertEquals(addonMarker, result.nextElement());
        assertFalse(result.hasMoreElements());
    }
}
