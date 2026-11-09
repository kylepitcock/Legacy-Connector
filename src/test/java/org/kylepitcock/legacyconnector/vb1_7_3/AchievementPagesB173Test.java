package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.achievement.*;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.src.ACPage;
import org.junit.jupiter.api.Test;

class AchievementPagesB173Test {
    @Test void pageRegistrationPreservesOriginalParentObjectsIdsAndAwardCriteria(){
        var top=new Achievement(91940,"pageTop",0,0,new ItemStack(340,1,0),Achievements.OPEN_INVENTORY);
        var child=new Achievement(91941,"pageChild",1,0,new ItemStack(340,1,0),top);
        var external=new Achievement(91942,"pageExternal",2,0,new ItemStack(340,1,0),child);
        var id=AdvancementsB173.idOf(child);var page=new ACPage("Example custom page");
        page.addAchievements(top,child);
        assertSame(page,ACPage.mcc$pageOf(top));assertSame(page,ACPage.mcc$pageOf(child));
        assertEquals(AdvancementsB173.pageRoot(page),AdvancementsB173.parentOf(top));
        assertEquals(AdvancementsB173.idOf(top),AdvancementsB173.parentOf(child));
        assertEquals(AdvancementsB173.rootOf(external.mcc$namespace),AdvancementsB173.parentOf(external));
        assertSame(top,child.parent);assertSame(child,external.parent);assertEquals(id,AdvancementsB173.idOf(child));
        var json=AdvancementsB173.definition(child,AdvancementsB173.parentOf(child),"minecraft:book");
        assertEquals("minecraft:impossible",json.getAsJsonObject("criteria").getAsJsonObject(AdvancementsB173.CRITERION).get("trigger").getAsString());
    }
    @Test void firstPageMembershipWinsLikeOriginalApiAndDistinctPagesHaveDistinctRoots(){
        var achievement=new Achievement(91943,"duplicatePageMember",0,0,new ItemStack(340,1,0),null);
        var first=new ACPage("First");var second=new ACPage("Second");
        first.addAchievements(achievement);second.addAchievements(achievement);
        assertSame(first,ACPage.mcc$pageOf(achievement));
        assertNotEquals(AdvancementsB173.pageRoot(first),AdvancementsB173.pageRoot(second));
        assertTrue(ACPage.mcc$pages().contains(first));assertEquals("First",first.title);
    }
    @Test void explicitCrossNamespaceMembershipUsesObjectIdentityWithoutNumericIdCollisions(){
        var previous=ContentB173.current();
        try {
            var metadata=new com.google.gson.JsonObject();metadata.addProperty("modernId","foreign_page_test");
            ContentB173.enter(new org.kylepitcock.legacyconnector.core.LegacyModRecord("b1.7.3","foreign_page_test","example",null,null,metadata));
            var foreign=new Achievement(91944,"foreign",0,0,new ItemStack(340,1,0),null);
            ContentB173.enter(null);
            var sameId=new Achievement(91944,"local",0,0,new ItemStack(340,1,0),null);
            var page=new ACPage("Cross namespace");page.addAchievements(foreign,Achievements.OPEN_INVENTORY);
            assertSame(page,ACPage.mcc$pageOf(foreign));assertNull(ACPage.mcc$pageOf(sameId));
            assertSame(page,ACPage.mcc$pageOf(Achievements.OPEN_INVENTORY));
            assertEquals(AdvancementsB173.pageRoot(page),AdvancementsB173.parentOf(foreign));
            assertNotEquals(page.mcc$namespace,foreign.mcc$namespace);
        } finally {ContentB173.enter(previous);}
    }
}
