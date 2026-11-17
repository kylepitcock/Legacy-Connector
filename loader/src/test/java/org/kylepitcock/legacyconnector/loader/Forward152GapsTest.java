package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardMembers;
import org.kylepitcock.legacyconnector.loader.synth.LegacyFields;
import org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData;
import org.kylepitcock.legacyconnector.loader.synth.LegacyMethods;

/** The 1.5.2 / 1.4.7 API gap rows of forward/1.5.2/member-manual.tsv (tools/dev/agent-tasks/1.5.2-gaps.md): the renames reach 1.7.10 members, the call rules are loaded for both versions. */
class Forward152GapsTest {
    private static final LegacyHierarchyData H17 = LegacyHierarchyData.forVersion(LegacyVersion.V1_7_10);
    private static final Map<String, Map<String, List<LegacyFields.Field>>> FIELDS = new java.util.HashMap<>();

    private static boolean hasMethod(String owner, String name, String desc) {
        for (var m : LegacyMethods.of(LegacyVersion.V1_7_10, owner)) if (m.name().equals(name) && m.desc().equals(desc)) return true;
        return false;
    }

    private static boolean hasField(String owner, String name) {
        for (var f : FIELDS.computeIfAbsent("1.7.10", LegacyFields::byOwner).getOrDefault(owner, List.of())) if (f.name().equals(name)) return true;
        return false;
    }

    private void renamesMethod(LegacyVersion v, String owner, String name, String desc, String target) {
        assertEquals(target, ForwardMembers.forVersion(v).method(owner, name, desc, null), v + " " + owner + "." + name);
        assertTrue(hasMethod(owner, target, desc) || hasMethod(owner, target, desc.replace("EntityLivingBase", "EntityLivingBase")), "1.7.10 has no " + owner + "." + target + desc);
    }

    @Test
    void methodRenamesReachRealMembers() {
        for (LegacyVersion v : List.of(LegacyVersion.V1_5_2, LegacyVersion.V1_4_7)) {
            renamesMethod(v, "net/minecraft/world/Explosion", "func_94613_c", "()Lnet/minecraft/entity/EntityLivingBase;", "getExplosivePlacedBy");
            renamesMethod(v, "net/minecraft/tileentity/TileEntity", "func_85027_a", "(Lnet/minecraft/crash/CrashReportCategory;)V", "addInfoToCrashReport");
            renamesMethod(v, "net/minecraft/tileentity/TileEntityChest", "func_98041_l", "()I", "getChestType");
            renamesMethod(v, "net/minecraft/command/CommandBase", "func_82359_c", "(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;)Lnet/minecraft/entity/player/EntityPlayerMP;", "getPlayer");
            renamesMethod(v, "net/minecraft/scoreboard/Scoreboard", "func_96508_e", "(Ljava/lang/String;)Lnet/minecraft/scoreboard/ScorePlayerTeam;", "getPlayersTeam");
            renamesMethod(v, "net/minecraft/entity/player/EntityPlayer", "func_96122_a", "(Lnet/minecraft/entity/player/EntityPlayer;)Z", "canAttackPlayer");
            renamesMethod(v, "net/minecraft/inventory/Container", "func_94531_b", "(Lnet/minecraft/inventory/Slot;)Z", "canDragIntoSlot");
            renamesMethod(v, "net/minecraft/tileentity/TileEntityHopper", "isCoolingDown", "()Z", "isOnTransferCooldown");
        }
    }

    @Test
    void hangingFieldsAreRenamed() {
        for (LegacyVersion v : List.of(LegacyVersion.V1_5_2, LegacyVersion.V1_4_7)) {
            ForwardMembers t = ForwardMembers.forVersion(v);
            assertEquals("field_146063_b", t.field("net/minecraft/entity/EntityHanging", "xPosition", null));
            assertEquals("field_146064_c", t.field("net/minecraft/entity/EntityHanging", "yPosition", null));
            assertEquals("field_146062_d", t.field("net/minecraft/entity/EntityHanging", "zPosition", null));
            assertTrue(hasField("net/minecraft/entity/EntityHanging", "field_146063_b"));
        }
    }

    @Test
    void constructorAndCallRulesAreLoaded() {
        for (LegacyVersion v : List.of(LegacyVersion.V1_5_2, LegacyVersion.V1_4_7)) {
            ForwardMembers t = ForwardMembers.forVersion(v);
            for (String key : List.of(
                    "net/minecraft/entity/ai/EntityAIMoveTowardsRestriction.<init>(Lnet/minecraft/entity/EntityCreature;F)V",
                    "net/minecraft/entity/ai/EntityAIMoveThroughVillage.<init>(Lnet/minecraft/entity/EntityCreature;FZ)V",
                    "net/minecraft/entity/ai/EntityAIMoveTowardsTarget.<init>(Lnet/minecraft/entity/EntityCreature;FF)V",
                    "net/minecraftforge/event/entity/living/EnderTeleportEvent.<init>(Lnet/minecraft/entity/EntityLivingBase;DDDI)V",
                    "net/minecraftforge/liquids/LiquidTank.<init>(IIILnet/minecraft/tileentity/TileEntity;)V",
                    "net/minecraft/src/ModTextureAnimation.<init>(IILjava/lang/String;Ljava/awt/image/BufferedImage;I)V")) {
                String[] rule = t.constructorRules().get(key);
                assertNotNull(rule, v + " has no constructor rule for " + key);
                assertEquals("args", rule[4]);
            }
            assertNotNull(t.callRules().get("net/minecraft/client/renderer/RenderGlobal.drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;ILnet/minecraft/item/ItemStack;F)V"));
            assertNotNull(t.callRules().get("cpw/mods/fml/common/ObfuscationReflectionHelper.setPrivateValue(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V"));
            assertNotNull(t.callRules().get("cpw/mods/fml/client/registry/RenderingRegistry.getUniqueTextureIndex(Ljava/lang/String;)I"));
            assertNotNull(t.fieldCallRules().get("net/minecraft/block/BlockLeaves.LEAF_TYPES"));
            assertNotNull(t.fieldCallRules().get("cpw/mods/fml/common/ObfuscationReflectionHelper.obfuscation"));
        }
    }

    @Test
    void worldConstructorSwapsSettingsAndProvider() {
        String[] rule = ForwardMembers.forVersion(LegacyVersion.V1_5_2).constructorRules().get(
                "net/minecraft/world/World.<init>(Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/WorldProvider;Lnet/minecraft/profiler/Profiler;Lnet/minecraft/logging/ILogAgent;)V");
        assertNotNull(rule);
        assertEquals(List.of("0", "1", "3", "2", "4"), List.of(rule).subList(5, rule.length));
        assertNotNull(H17.get("net/minecraft/world/World"));
    }
}
