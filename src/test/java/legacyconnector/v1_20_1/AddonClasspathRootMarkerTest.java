package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/** Ensures VanillaPackResourcesBuilder can see the additional addon resource root on the game classpath. */
class AddonClasspathRootMarkerTest {
    @Test
    void exposesTheAddonRootMarkerFromTheRuntimeModResources() {
        assertNotNull(
                AddonClasspathRootMarkerTest.class.getClassLoader().getResource("addon/.mcassetsroot"),
                "the packaged Legacy Connector runtime must expose addon/.mcassetsroot");
    }
}
