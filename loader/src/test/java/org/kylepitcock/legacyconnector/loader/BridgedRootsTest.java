package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The Waila, NEI and Baubles bridges relocate the legacy API into the shim whatever the legacy version: the package roots are the same
 * on 1.4.7 to 1.12.2, and the forward versions link against the 1.7.10 shim where those API classes live.
 */
class BridgedRootsTest {
    @AfterEach
    void reset() {
        LegacyVersion.setBridgedRoots(List.of());
    }

    @Test
    void apiPackagesOfAllVersionsAreRelocated() {
        LegacyVersion.setBridgedRoots(LibraryBridges.KNOWN.stream().map(LibraryBridges.Bridge::apiRoot).toList());
        for (String cls : List.of("baubles/api/IBauble", "baubles/api/BaublesApi", "baubles/common/lib/PlayerHandler",
                "mcp/mobius/waila/api/IWailaDataProvider", "mcp/mobius/waila/api/IWailaPlugin", "mcp/mobius/waila/api/impl/ModuleRegistrar",
                "codechicken/nei/api/API", "codechicken/nei/recipe/TemplateRecipeHandler", "codechicken/nei/api/IConfigureNEI")) {
            assertTrue(LegacyVersion.isRelocatedRoot(cls), cls);
        }
        // a mod's own package that merely looks similar stays where it is
        assertFalse(LegacyVersion.isRelocatedRoot("com/example/baubles/api/IBauble"));
    }

    @Test
    void forwardVersionsLinkAgainstTheShimOf1710() {
        for (LegacyVersion v : List.of(LegacyVersion.V1_4_7, LegacyVersion.V1_5_2, LegacyVersion.V1_6_4, LegacyVersion.V1_7_10)) {
            assertEquals("legacyconnector/v1_7_10/", v.linkPrefix(), v.id());
        }
        assertEquals("legacyconnector/v1_12_2/", LegacyVersion.V1_12_2.linkPrefix());
    }

    @Test
    void everyBridgeHasAConfigSwitchName() {
        for (LibraryBridges.Bridge b : LibraryBridges.KNOWN) assertTrue(b.configKey() != null && !b.configKey().isBlank(), b.apiRoot());
        assertTrue(LibraryBridges.KNOWN.stream().anyMatch(b -> "waila".equals(b.configKey())));
        assertTrue(LibraryBridges.KNOWN.stream().anyMatch(b -> "nei".equals(b.configKey())));
        assertTrue(LibraryBridges.KNOWN.stream().anyMatch(b -> "baubles".equals(b.configKey())));
    }
}
