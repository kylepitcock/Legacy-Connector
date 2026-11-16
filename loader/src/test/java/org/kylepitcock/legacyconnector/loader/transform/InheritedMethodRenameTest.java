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

    /** TileTeleportEnergy: a sibling subclass that overrides the base method without implementing the interface. */
    @Test
    void siblingOverrideFollowsTheRenamedBase() {
        LegacyRemapper r = new LegacyRemapper(LegacyVersion.V1_4_7, McpMappings.forVersion(LegacyVersion.V1_4_7), null, null);
        r.notchLocals(Map.of(
                "mod/Root", new String[] {"java/lang/Object"},
                "mod/Item", new String[] {"mod/Root", "net/minecraftforge/common/ISidedInventory"},
                "mod/Energy", new String[] {"mod/Root"},
                "mod/Deeper", new String[] {"mod/Energy"}));
        r.localMethods(Map.of("mod/Root", Set.of("b" + DESC), "mod/Item", Set.of("b" + DESC), "mod/Energy", Set.of("b" + DESC), "mod/Deeper", Set.of("b" + DESC)));
        assertEquals("getInventoryName", r.mapMethodName("mod/Energy", "b", DESC));
        assertEquals("getInventoryName", r.mapMethodName("mod/Deeper", "b", DESC));
        assertEquals("getInventoryName", r.mapMethodName("mod/Root", "b", DESC));
    }

    /** A call through a subclass that inherits the declaration, and a local interface declaring the method. */
    @Test
    void inheritedCallsAndLocalInterfaces() {
        LegacyRemapper r = new LegacyRemapper(LegacyVersion.V1_4_7, McpMappings.forVersion(LegacyVersion.V1_4_7), null, null);
        r.notchLocals(Map.of(
                "mod/Named", new String[] {"java/lang/Object"},
                "mod/Impl", new String[] {"java/lang/Object", "mod/Named", "net/minecraftforge/common/ISidedInventory"},
                "mod/Plain", new String[] {"java/lang/Object", "mod/Named"},
                "mod/Sub", new String[] {"mod/Plain"}));
        r.localMethods(Map.of("mod/Named", Set.of("b" + DESC), "mod/Impl", Set.of("b" + DESC), "mod/Plain", Set.of("b" + DESC)));
        assertEquals("getInventoryName", r.mapMethodName("mod/Named", "b", DESC));
        assertEquals("getInventoryName", r.mapMethodName("mod/Plain", "b", DESC));
        assertEquals("getInventoryName", r.mapMethodName("mod/Sub", "b", DESC)); // inherited call
    }
}
