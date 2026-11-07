package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames;
import org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData;

class Forward152Test {
    @Test
    void tablesChainOver164() {
        assertEquals(List.of("1.6.4", "1.5.2"), LegacyVersion.V1_5_2.forwardTableIds());
        assertSame(LegacyVersion.V1_7_10, LegacyVersion.V1_5_2.forward());
        assertSame(LegacyVersion.V1_5_2, LegacyVersion.fromMinecraftVersion("1.5.2"));
        // the 1.6.4 renames still apply, the 1.5.2 ones are added
        assertEquals("net/minecraft/util/IIcon", ForwardRenames.map(LegacyVersion.V1_5_2, "net/minecraft/util/Icon"));
        assertEquals("net/minecraft/entity/EntityLivingBase", ForwardRenames.map(LegacyVersion.V1_5_2, "net/minecraft/entity/EntityLiving"));
        assertEquals("net/minecraft/entity/EntityLiving", ForwardRenames.map(LegacyVersion.V1_6_4, "net/minecraft/entity/EntityLiving"));
    }

    @Test
    void renameTargetsExistIn1710() {
        var h17 = LegacyHierarchyData.forVersion(LegacyVersion.V1_7_10);
        // (the 1.6.4 rows underneath name classes 1.6.4 has and 1.5.2 may not: only the targets are checked)
        ForwardRenames.forVersion(LegacyVersion.V1_5_2).forEach((from, to) -> {
            if (!to.startsWith("net/minecraft/src/")) assertNotNull(h17.get(to), "1.7.10 has no " + to);   // net/minecraft/src/ModLoader and BaseMod are shim classes
        });
        assertTrue(ForwardRenames.forVersion(LegacyVersion.V1_5_2).size() > 50);
    }
}
