package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;

/** FML leaves the root folders assets and data out of a mod's module, so classes there must move (Rival Rebels: assets.rivalrebels.RivalRebels). */
class HiddenRootPackagesTest {
    @Test
    void classesUnderAssetsMove() {
        CrossVersionPackages packages = new CrossVersionPackages();
        packages.add(LegacyVersion.V1_7_10, "assets/rivalrebels/RivalRebels");
        packages.add(LegacyVersion.V1_7_10, "assets/rivalrebels/common/Util");
        packages.add(LegacyVersion.V1_7_10, "org/example/Plain");
        packages.modern(Set.of(), Set.of()).done();
        assertEquals("legacyconnector_relocated/v1_7_10/assets/rivalrebels/RivalRebels", packages.map(LegacyVersion.V1_7_10, "assets/rivalrebels/RivalRebels"));
        assertEquals("legacyconnector_relocated/v1_7_10/assets/rivalrebels/common/Util", packages.map(LegacyVersion.V1_7_10, "assets/rivalrebels/common/Util"));
        assertEquals(null, packages.map(LegacyVersion.V1_7_10, "org/example/Plain"));
    }
}
