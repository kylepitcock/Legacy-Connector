package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.McpMappings;

/**
 * A notch-named 1.4.7 jar whose abstract base class declares a method that only a subclass overrides from Minecraft (Thermal Expansion:
 * TileTeleportRoot.b() returns null, TileTeleportItem.b() is IInventory.getInvName): the base declaration and calls through the base type take
 * the subclass's final name, or the call reaches the base method.
 */
class InheritedMethodRenameTest {
    private static final String DESC = "()Ljava/lang/String;";

    @Test
    void baseClassDeclarationFollowsTheSubclassRename() {
        LegacyRemapper r = new LegacyRemapper(LegacyVersion.V1_4_7, McpMappings.forVersion(LegacyVersion.V1_4_7), null, null);
        r.notchLocals(Map.of(
                "mod/Root", new String[] {"java/lang/Object"},
                "mod/Item", new String[] {"mod/Root", "net/minecraftforge/common/ISidedInventory"}));
        r.localMethods(Map.of("mod/Root", Set.of("b" + DESC), "mod/Item", Set.of("b" + DESC)));
        String onItem = r.mapMethodName("mod/Item", "b", DESC);
        assertEquals("getInventoryName", onItem);
        assertEquals(onItem, r.mapMethodName("mod/Root", "b", DESC));
    }

    @Test
    void unrelatedBaseMethodKeepsItsName() {
        LegacyRemapper r = new LegacyRemapper(LegacyVersion.V1_4_7, McpMappings.forVersion(LegacyVersion.V1_4_7), null, null);
        r.notchLocals(Map.of(
                "mod/Root", new String[] {"java/lang/Object"},
                "mod/Item", new String[] {"mod/Root", "net/minecraftforge/common/ISidedInventory"}));
        r.localMethods(Map.of("mod/Root", Set.of("zz" + DESC), "mod/Item", Set.of("b" + DESC)));
        assertEquals("zz", r.mapMethodName("mod/Root", "zz", DESC));
    }
}
