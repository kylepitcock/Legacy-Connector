package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BetaDisplayNameTest {
    @Test
    void archiveNameCarryingTheModNameIsUsedWithoutVersion() {
        assertEquals("NetherCraft", BetaJars.displayName(Path.of("mods/NetherCraftv1.9b.zip"), "Nether"));
        assertEquals("NetherCraft", BetaJars.displayName(Path.of("NetherCraftv1.9b_Doesnt_require_ScotTools_Modloader_only_b173.zip".substring(0, 0) + "NetherCraft_v1.9b_b173.zip"), "Nether"));
    }

    @Test
    void guidNamedOrUnrelatedFilesFallBackToTheClassName() {
        assertEquals("Nether", BetaJars.displayName(Path.of("3479ae77-b70c-4a72-baa3-e6bdf1080016.jar"), "Nether"));
        assertEquals("Nether", BetaJars.displayName(Path.of("random_pack.zip"), "Nether"));
    }
}
