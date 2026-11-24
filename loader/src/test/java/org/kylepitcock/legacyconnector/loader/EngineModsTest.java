package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The engine-only mods table ({@code engine-mods.tsv}) drives both the loader's skip list and the compatibility site's "engine" status. */
class EngineModsTest {
    private static LegacyJar jar(LegacyVersion version, String... ids) {
        LegacyJar j = new LegacyJar(Path.of("mod.jar"));
        j.version = version;
        for (String id : ids) {
            LegacyModDecl m = new LegacyModDecl();
            m.legacyId = id;
            j.mods.add(m);
        }
        return j;
    }

    @Test
    void tableIsWellFormed() {
        var table = LibraryBridges.engineMods();
        assertTrue(table.size() > 60);
        Set<String> known = Set.of("b1.7.3", "1.2.5", "1.4.7", "1.5.2", "1.6.4", "1.7.10", "1.12.2", "1.20.1");
        for (var e : table.values()) {
            assertFalse(e.versions().isEmpty(), e.id());
            assertTrue(known.containsAll(e.versions()), e.id() + " " + e.versions());
            assertFalse(e.reason().isBlank(), e.id());
            assertEquals(e.id(), e.id().toLowerCase(java.util.Locale.ROOT));
        }
        assertTrue(LibraryBridges.obsoleteIds().contains("fastcraft"));
    }

    @Test
    void oldEntriesKeepSkippingAndKeepTheirReason() {
        assertEquals("1.7.10 performance patches; not applicable to 1.21.1", LibraryBridges.obsoleteReason(jar(LegacyVersion.V1_7_10, "FastCraft")));
        assertNotNull(LibraryBridges.obsoleteReason(jar(LegacyVersion.V1_12_2, "foamfix")));
    }

    @Test
    void engineModsOf1201AreSkippedOnlyOnThatVersion() {
        assertNotNull(LibraryBridges.obsoleteReason(jar(LegacyVersion.V1_20_1, "embeddium")));
        assertNotNull(LibraryBridges.obsoleteReason(jar(LegacyVersion.V1_20_1, "oculus")));
        // a 1.7.10 mod that happens to use the id is not an engine mod of 1.20.1
        assertNull(LibraryBridges.obsoleteReason(jar(LegacyVersion.V1_7_10, "embeddium")));
        // every mod of the jar must be on the list
        assertNull(LibraryBridges.obsoleteReason(jar(LegacyVersion.V1_20_1, "embeddium", "somethingelse")));
    }

    @Test
    void clientConveniencesAreNotEngineMods() {
        for (String id : new String[] {"inventorytweaks", "journeymap", "custommainmenu", "mousetweaks", "ctm", "xaerominimap"}) {
            assertNull(LibraryBridges.engineMods().get(id), id);
        }
    }
}
