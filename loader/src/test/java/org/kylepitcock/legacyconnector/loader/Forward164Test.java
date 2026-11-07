package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames;
import org.kylepitcock.legacyconnector.loader.mapping.NotchNames;
import org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData;

class Forward164Test {
    @Test
    void versionStringsPickTheRightFamily() {
        assertSame(LegacyVersion.V1_6_4, LegacyVersion.fromMinecraftVersion("[1.6.4]"));
        assertSame(LegacyVersion.V1_6_4, LegacyVersion.fromMinecraftVersion("1.6.2"));
        assertSame(LegacyVersion.V1_7_10, LegacyVersion.fromMinecraftVersion("1.7.10"));
        assertSame(LegacyVersion.V1_12_2, LegacyVersion.fromMinecraftVersion("1.12.2"));
        assertSame(LegacyVersion.V1_7_10, LegacyVersion.V1_6_4.forward());
        assertNull(LegacyVersion.V1_7_10.forward());
        assertEquals("legacyconnector/v1_7_10/", LegacyVersion.V1_6_4.linkPrefix());
        assertEquals("legacyconnector/v1_6_4/", LegacyVersion.V1_6_4.shimPrefix());
    }

    @Test
    void renameTargetsExistIn1710() {
        var h17 = LegacyHierarchyData.forVersion(LegacyVersion.V1_7_10);
        var h16 = LegacyHierarchyData.forVersion(LegacyVersion.V1_6_4);
        var table = ForwardRenames.forVersion(LegacyVersion.V1_6_4);
        assertTrue(table.size() > 50);
        table.forEach((from, to) -> {
            // nested classes jars reference but the generated hierarchy table (top-level API of the Forge sources) lacks
            if (!from.equals("net/minecraftforge/common/Configuration$UnicodeInputStreamReader")) assertNotNull(h16.get(from), "1.6.4 has no " + from);
            assertNotNull(h17.get(to), "1.7.10 has no " + to);
        });
        assertEquals("net/minecraft/util/IIcon", ForwardRenames.map(LegacyVersion.V1_6_4, "net/minecraft/util/Icon"));
        assertEquals("net/minecraft/util/Icon", ForwardRenames.map(LegacyVersion.V1_7_10, "net/minecraft/util/Icon"));
    }

    @Test
    void notchNamesResolveThroughSupertypes() {
        NotchNames n = NotchNames.get();
        assertEquals("net/minecraft/block/Block", n.mcpClass("aqz"));
        assertNull(n.mcpClass("net"));
    }
}
