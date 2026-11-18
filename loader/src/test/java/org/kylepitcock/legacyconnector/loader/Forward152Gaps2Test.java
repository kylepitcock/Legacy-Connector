package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardMembers;
import org.kylepitcock.legacyconnector.loader.synth.LegacyFields;

/** API gap round 2 for 1.5.2 and 1.4.7 (tools/dev/agent-tasks/1.5.2-gaps.md, 1.4.7-gaps.md): the rows that were added to the forward tables. */
class Forward152Gaps2Test {
    @Test
    void directionTablesOf147AreRenamedToTheirValues() {
        ForwardMembers t = ForwardMembers.forVersion(LegacyVersion.V1_4_7);
        // the vanilla 1.4.7 class: the bed renderer reads headInvisibleFace {3,4,2,5} and footInvisibleFaceRemap {2,3,0,1}
        assertEquals("rotateOpposite", t.field("net/minecraft/util/Direction", "footInvisibleFaceRemap", null));
        assertEquals("directionToFacing", t.field("net/minecraft/util/Direction", "headInvisibleFace", null));
        assertEquals("rotateRight", t.field("net/minecraft/util/Direction", "enderEyeMetaToDirection", null));
        assertEquals("rotateLeft", t.field("net/minecraft/util/Direction", "field_71578_g", null));
        var fields = LegacyFields.byOwner("1.7.10").getOrDefault("net/minecraft/util/Direction", List.of());
        for (String target : List.of("rotateOpposite", "directionToFacing", "rotateRight", "rotateLeft")) {
            assertTrue(fields.stream().anyMatch(f -> f.name().equals(target)), "1.7.10 Direction has no " + target);
        }
    }

    @Test
    void commandAndSoundCallsOf147AreRetargeted() {
        ForwardMembers t = ForwardMembers.forVersion(LegacyVersion.V1_4_7);
        assertNotNull(t.callRules().get("net/minecraft/command/ICommandManager.executeCommand(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;)V"));
        assertNotNull(t.callRules().get("net/minecraft/client/multiplayer/WorldClient.playSound(DDDLjava/lang/String;FF)V"));
    }

    private static String resource(String path) throws Exception {
        try (InputStream in = Forward152Gaps2Test.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void blockHooksAreBridged() throws Exception {
        String rows = resource("legacyconnector/forward/1.5.2/bridge-methods.tsv");
        assertTrue(rows.contains("isLadder\t(Lnet/minecraft/world/World;III)Z\tisLadder\t(Lnet/minecraft/world/IBlockAccess;IIILnet/minecraft/entity/EntityLivingBase;)Z\t0,1,2,3"));
        assertTrue(rows.contains("isBlockSolidOnSide\t(Lnet/minecraft/world/World;IIILnet/minecraftforge/common/ForgeDirection;)Z\tisSideSolid"));
        assertTrue(rows.contains("canCreatureSpawn\t(Lnet/minecraft/entity/EnumCreatureType;Lnet/minecraft/world/World;III)Z\tcanCreatureSpawn"));
    }
}
