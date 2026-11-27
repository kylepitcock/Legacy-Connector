package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * The loader is the top-level jar discovered before the embedded runtime mod.
 * Vanilla's early addon-root scan uses the loader-side class path, so the marker
 * must be packaged in this module as well as in the runtime mod.
 */
class AddonResourceRootMarkerTest {
    @Test
    void addonResourceRootMarkerIsVisibleFromTheLoaderClasspath() {
        assertNotNull(
                getClass().getClassLoader().getResource("addon/.mcassetsroot"),
                "the top-level loader jar must expose the addon root marker during early resource discovery");
    }
}
