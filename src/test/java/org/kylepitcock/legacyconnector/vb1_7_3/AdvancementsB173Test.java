package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.achievement.*;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

class AdvancementsB173Test {
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
