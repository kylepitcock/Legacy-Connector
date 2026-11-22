package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;

/** {@link CrossVersionPackages}: packages two legacy versions both define keep the oldest version's names, the others are relocated. */
class CrossVersionPackagesTest {
    private static final LegacyVersion OLD = LegacyVersion.V1_7_10;
    private static final LegacyVersion NEW = LegacyVersion.V1_12_2;

    private static CrossVersionPackages shared() {
        CrossVersionPackages c = new CrossVersionPackages();
        c.add(OLD, "codechicken/lib/Vec");
        c.add(NEW, "codechicken/lib/Vec");
        c.add(NEW, "codechicken/lib/Only12");
        c.add(OLD, "mod/own/Thing"); // only one version: not shared
        return c.done();
    }

    @Test
    void emptyInstanceMapsNothing() {
        assertTrue(CrossVersionPackages.NONE.isEmpty());
        assertNull(CrossVersionPackages.NONE.map(OLD, "a/B"));
        assertTrue(CrossVersionPackages.NONE.relocated(OLD).isEmpty());
    }

    @Test
    void onlyPackagesOfSeveralVersionsAreKept() {
        CrossVersionPackages c = shared();
        assertFalse(c.isEmpty());
        assertNull(c.map(OLD, "mod/own/Thing"));
        assertNull(c.map(NEW, "mod/own/Thing"));
        assertEquals(Set.of("codechicken/lib"), c.relocated(NEW));
        assertTrue(c.relocated(OLD).isEmpty(), "the oldest version keeps the names");
    }

    @Test
    void theOldestVersionKeepsItsNamesAndTheNewerOneIsRelocated() {
        CrossVersionPackages c = shared();
        assertEquals("codechicken/lib/Vec", c.map(OLD, "codechicken/lib/Vec"));
        assertEquals(CrossVersionPackages.prefix(NEW) + "codechicken/lib/Vec", c.map(NEW, "codechicken/lib/Vec"));
        assertEquals("legacyconnector_relocated/v1_12_2/", CrossVersionPackages.prefix(NEW));
    }

    @Test
    void aClassOnlyAnotherVersionHasLinksToThatVersionsCopy() {
        CrossVersionPackages c = shared();
        // the 1.7.10 jars reference a class only the 1.12.2 copy of the package has: it links there, not to a missing 1.7.10 class
        assertEquals(CrossVersionPackages.prefix(NEW) + "codechicken/lib/Only12", c.map(OLD, "codechicken/lib/Only12"));
        assertEquals(CrossVersionPackages.prefix(NEW) + "codechicken/lib/Only12", c.map(NEW, "codechicken/lib/Only12"));
    }

    @Test
    void nestedClassesAndDefaultPackageNamesAreHandledWhole() {
        CrossVersionPackages c = new CrossVersionPackages();
        c.add(OLD, "a/b/C$D");
        c.add(NEW, "a/b/C$D");
        c.add(OLD, "NoPackage");
        c.add(NEW, "NoPackage");
        c.done();
        assertEquals(CrossVersionPackages.prefix(NEW) + "a/b/C$D", c.map(NEW, "a/b/C$D"));
        assertNull(c.map(NEW, "NoPackage"), "classes of the default package are never relocated");
    }

    @Test
    void modernClashesAreRelocatedInEveryLegacyVersionButTheKeptPackages() {
        CrossVersionPackages c = new CrossVersionPackages();
        c.add(OLD, "appeng/api/IAE");
        c.add(OLD, "appeng/api/Other");
        c.add(OLD, "mod/Main");
        c.modern(Set.of("appeng/api", "mod"), Set.of("mod")).done();
        assertEquals(Set.of("appeng/api"), c.modernClashes());
        assertEquals(Set.of("appeng/api"), c.relocated(OLD));
        assertEquals(CrossVersionPackages.prefix(OLD) + "appeng/api/IAE", c.map(OLD, "appeng/api/IAE"));
        assertNull(c.map(OLD, "mod/Main"));
    }

    @Test
    void aClassOnlyTheModernModHasKeepsItsName() {
        CrossVersionPackages c = new CrossVersionPackages();
        c.add(OLD, "appeng/api/Old");
        c.modern(Set.of("appeng/api"), Set.of()).done();
        assertEquals("appeng/api/ModernOnly", c.map(OLD, "appeng/api/ModernOnly"));
    }

    @Test
    void fingerprintChangesWhenAnotherVersionsJarsDo() {
        CrossVersionPackages a = shared();
        CrossVersionPackages b = new CrossVersionPackages();
        b.add(OLD, "codechicken/lib/Vec");
        b.add(NEW, "codechicken/lib/Vec");
        b.done();
        assertFalse(a.fingerprint().equals(b.fingerprint()), "an added class of a shared package is part of the key");
        CrossVersionPackages c = shared();
        assertEquals(a.fingerprint(), c.fingerprint(), "and it is stable");
    }

    @Test
    void versionsThatShareAShimStillGetSeparateRelocationPrefixes() {
        CrossVersionPackages c = new CrossVersionPackages();
        for (LegacyVersion v : new LegacyVersion[] {LegacyVersion.V1_4_7, LegacyVersion.V1_5_2, LegacyVersion.V1_6_4}) c.add(v, "lib/api/Thing");
        c.done();
        String a = c.map(LegacyVersion.V1_5_2, "lib/api/Thing");
        String b = c.map(LegacyVersion.V1_6_4, "lib/api/Thing");
        assertEquals("lib/api/Thing", c.map(LegacyVersion.V1_4_7, "lib/api/Thing"));
        assertFalse(a.equals(b), "1.5.2 and 1.6.4 copies must not land on the same name: " + a);
        assertEquals("legacyconnector_relocated/v1_7_10/", CrossVersionPackages.prefix(LegacyVersion.V1_7_10));
    }
}
