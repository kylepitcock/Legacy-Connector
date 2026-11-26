package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/** Ensures the nonstandard addon root is visible during Minecraft's early classpath scan. */
class AddonResourceRootMarkerTest {
    @Test
    void addonResourceRootMarkerIsPresentOnTheConnectorClasspath() {
        assertNotNull(getClass().getClassLoader().getResource("addon/.mcassetsroot"));
    }
}
