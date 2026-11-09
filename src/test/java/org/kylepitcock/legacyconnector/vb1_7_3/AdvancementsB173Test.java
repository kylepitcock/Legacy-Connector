package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.achievement.*;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

class AdvancementsB173Test {
    @Test void furnaceAwardsRetainBetaIdentitiesAndUseOriginalCallbackCriteria() {
        var iron=Achievements.ACQUIRE_IRON;var fish=Achievements.COOK_FISH;
        assertEquals(5242885,iron.id);assertEquals("achievement.acquireIron",iron.stringId);
        assertEquals(1,iron.column);assertEquals(4,iron.row);assertEquals(265,iron.icon.itemId);
        assertEquals(5242890,fish.id);assertEquals("achievement.cookFish",fish.stringId);
        assertEquals(2,fish.column);assertEquals(6,fish.row);assertEquals(350,fish.icon.itemId);
        assertSame(Achievements.BUILD_FURNACE,iron.parent);assertSame(iron.parent,fish.parent);
        assertSame(Achievements.BUILD_PICKAXE,iron.parent.parent);
        assertSame(Achievements.BUILD_WORKBENCH,iron.parent.parent.parent);
        assertSame(Achievements.MINE_WOOD,iron.parent.parent.parent.parent);
        assertSame(Achievements.OPEN_INVENTORY,iron.parent.parent.parent.parent.parent);
        for(var a:java.util.List.of(Achievements.MINE_WOOD,Achievements.BUILD_WORKBENCH,Achievements.BUILD_PICKAXE,Achievements.BUILD_FURNACE,iron,fish)){
            assertTrue(a.mcc$vanilla);assertEquals("legacyconnector",a.mcc$namespace);
            assertTrue(AdvancementsB173.all().contains(a));assertTrue(Achievements.ACHIEVEMENTS.contains(a));
            var id=AdvancementsB173.idOf(a);assertEquals("legacyconnector",id.getNamespace());
            assertEquals(a.parent==Achievements.OPEN_INVENTORY?AdvancementsB173.rootOf("legacyconnector"):AdvancementsB173.idOf(a.parent),AdvancementsB173.parentOf(a));
            var definition=AdvancementsB173.definition(a,AdvancementsB173.parentOf(a),"minecraft:book");
            assertEquals("minecraft:impossible",definition.getAsJsonObject("criteria").getAsJsonObject(AdvancementsB173.CRITERION).get("trigger").getAsString());
        }
        assertTrue(Achievements.OPEN_INVENTORY.localOnly);
        assertEquals("minecraft:story/root",AdvancementsB173.idOf(Achievements.OPEN_INVENTORY).toString());
    }
    @Test void registeredAchievementsCannotGenerateBetaPackOnModernOnlyInstance() {
        assertTrue(org.kylepitcock.legacyconnector.core.LegacyMods.ordered().isEmpty());
        new Achievement(91953,"registeredWithoutLoadedBetaMod",0,0,new ItemStack(340,1,0),Achievements.OPEN_INVENTORY).addStat();
        assertTrue(AdvancementsB173.all().contains(Achievements.COOK_FISH));
        var out=new java.util.LinkedHashMap<String,java.util.function.Supplier<byte[]>>();
        AdvancementsB173.contribute(out);
        assertTrue(out.isEmpty());
        assertSame(Achievements.OPEN_INVENTORY,Achievements.MINE_WOOD.parent);
        assertEquals("legacyconnector:beta/root",AdvancementsB173.parentOf(Achievements.MINE_WOOD).toString());
        assertEquals("minecraft:story/root",AdvancementsB173.idOf(Achievements.OPEN_INVENTORY).toString());
    }
    @Test void originalConstructorsRegistrationAndDescriptionProduceNativeDefinition() {
        var achievement=new Achievement(91821,"example",-4,-5,new ItemStack(340,1,0),Achievements.OPEN_INVENTORY).challenge().addStat();
        assertSame(achievement,achievement.localOnly());assertTrue(achievement.isAchievement());assertTrue(achievement.localOnly);
        assertEquals(5242880+91821,achievement.id);assertSame(Achievements.OPEN_INVENTORY,achievement.parent);
        assertTrue(AdvancementsB173.all().contains(achievement));assertTrue(Achievements.ACHIEVEMENTS.contains(achievement));
        AdvancementsB173.describe(achievement,"Original title","Original instructions");
        assertEquals("Original instructions",achievement.getTranslatedDescription());
        var parent=AdvancementsB173.idOf(achievement.parent);
        assertEquals("minecraft:story/root",parent.toString());
        var json=AdvancementsB173.definition(achievement,parent,"minecraft:book");
        assertEquals("minecraft:story/root",json.get("parent").getAsString());
        assertEquals("Original title",json.getAsJsonObject("display").getAsJsonObject("title").get("text").getAsString());
        assertEquals("challenge",json.getAsJsonObject("display").get("frame").getAsString());
        assertEquals("minecraft:impossible",json.getAsJsonObject("criteria").getAsJsonObject(AdvancementsB173.CRITERION).get("trigger").getAsString());
    }
    @Test void stableNamespacedIdsDistinguishStatsAndParentsRemainOriginalObjects() {
        var first=new Achievement(91822,"Same Name",0,0,new ItemStack(340,1,0),null);
        var second=new Achievement(91823,"Same Name",1,0,new ItemStack(340,1,0),first);
        assertNotEquals(AdvancementsB173.idOf(first),AdvancementsB173.idOf(second));
        assertSame(first,second.parent);
        var root=AdvancementsB173.definition(first,null,"minecraft:book");
        assertFalse(root.has("parent"));assertTrue(root.getAsJsonObject("display").has("background"));
    }
    @Test void originalTopLevelAndVanillaParentsUseIndependentBetaCategoryWithoutChangingOriginalParents() {
        var top=new Achievement(91824,"top",0,0,new ItemStack(340,1,0),null);
        var vanillaChild=new Achievement(91825,"vanillaChild",1,0,new ItemStack(340,1,0),Achievements.OPEN_INVENTORY);
        var child=new Achievement(91826,"child",1,1,new ItemStack(340,1,0),top);
        assertEquals(AdvancementsB173.rootOf(top.mcc$namespace),AdvancementsB173.parentOf(top));
        assertEquals(AdvancementsB173.rootOf(vanillaChild.mcc$namespace),AdvancementsB173.parentOf(vanillaChild));
        assertEquals(AdvancementsB173.idOf(top),AdvancementsB173.parentOf(child));
        assertNull(top.parent);assertSame(Achievements.OPEN_INVENTORY,vanillaChild.parent);assertSame(top,child.parent);
        assertEquals("minecraft:story/root",AdvancementsB173.idOf(Achievements.OPEN_INVENTORY).toString());
        assertNotEquals(AdvancementsB173.rootOf("b173_example"),AdvancementsB173.rootOf("example"));
    }
    @Test void onlySyntheticCategoryUnlocksAutomaticallyAndOriginalAchievementKeepsItsStableId() {
        var original=new Achievement(91827,"earnedByOriginal",0,0,new ItemStack(340,1,0),null);
        var id=AdvancementsB173.idOf(original);
        var category=AdvancementsB173.rootDefinition("Example (Beta 1.7.3)","minecraft:book");
        assertFalse(category.has("parent"));
        assertEquals("minecraft:tick",category.getAsJsonObject("criteria").getAsJsonObject("visible").get("trigger").getAsString());
        assertFalse(category.getAsJsonObject("display").get("show_toast").getAsBoolean());
        assertFalse(category.getAsJsonObject("display").get("announce_to_chat").getAsBoolean());
        var definition=AdvancementsB173.definition(original,AdvancementsB173.parentOf(original),"minecraft:book");
        assertEquals("minecraft:impossible",definition.getAsJsonObject("criteria").getAsJsonObject(AdvancementsB173.CRITERION).get("trigger").getAsString());
        assertEquals(AdvancementsB173.rootOf(original.mcc$namespace).toString(),definition.get("parent").getAsString());
        assertEquals(id,AdvancementsB173.idOf(original));
    }
}
