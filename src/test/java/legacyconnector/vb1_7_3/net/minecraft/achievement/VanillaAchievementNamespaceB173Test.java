package legacyconnector.vb1_7_3.net.minecraft.achievement;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;

class VanillaAchievementNamespaceB173Test {
    @Test void explicitNamespaceDoesNotBorrowOrChangeActiveModOwnership() {
        var previous=ContentB173.current();
        try {
            var metadata=new com.google.gson.JsonObject();metadata.addProperty("modernId","achievement_namespace_test");
            var owner=new org.kylepitcock.legacyconnector.core.LegacyModRecord("b1.7.3","achievement_namespace_test","example",null,null,metadata);
            ContentB173.enter(owner);
            var vanilla=new Achievement(91951,"bridgeOwned",0,0,new ItemStack(340,1,0),null,"legacyconnector");
            var custom=new Achievement(91952,"modOwned",0,0,new ItemStack(340,1,0),null);
            assertEquals("legacyconnector",vanilla.mcc$namespace);
            assertEquals(owner.modernId,custom.mcc$namespace);
            assertSame(owner,ContentB173.current());
            assertEquals("legacyconnector",Achievements.ACQUIRE_IRON.mcc$namespace);
            assertEquals("legacyconnector",Achievements.COOK_FISH.mcc$namespace);
        } finally {ContentB173.enter(previous);}
    }
}
