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
}
