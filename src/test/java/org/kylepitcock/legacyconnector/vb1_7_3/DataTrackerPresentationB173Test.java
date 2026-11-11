package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.data.DataTracker;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3i;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DataTrackerPresentationB173Test {
    @BeforeAll static void bootstrap()throws Exception {NativeStackComponentsB173Test.bootstrap();}
    static class TrackedAnimal extends legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity {
        int phase=7;
        TrackedAnimal(){super(null);}
        @Override public void initDataTracker(){super.initDataTracker();dataTracker.startTracking(16,(byte)0);}
    }
    private static DataTracker allTypes(){
        var tracker=new DataTracker();tracker.startTracking(0,(byte)1);tracker.startTracking(1,(short)2);tracker.startTracking(2,3);
        tracker.startTracking(3,.5F);tracker.startTracking(4,"original");tracker.startTracking(5,new ItemStack(1,3,4));tracker.startTracking(6,new Vec3i(1,2,3));return tracker;
    }
    @Test void exactBetaTypesRoundtripWithIndependentMutableStackAndPositionValues(){
        var server=allTypes();var client=allTypes();var snapshot=server.mcc$snapshot();
        client.set(0,(byte)9);client.set(1,(short)9);client.set(2,9);client.set(3,9F);client.set(4,"local");
        client.mcc$prepare(null,snapshot).run();
        assertEquals(1,client.getByte(0));assertEquals(2,client.getShort(1));assertEquals(3,client.getInt(2));assertEquals(.5F,client.getFloat(3));assertEquals("original",client.getString(4));
        assertEquals(snapshot,client.mcc$snapshot());assertTrue(client.isDirty(),"Presentation replay does not consume an existing local dirty marker");
        server.set(5,new ItemStack(1,7,9));server.set(6,new Vec3i(9,8,7));
        assertEquals(snapshot,client.mcc$snapshot(),"Independent entity trackers must not share mutable decoded values");
    }
    @Test void ownedHostMetadataHydratesFlagsAndPreservesUnchangedClientRenderChoices(){
        var server=new TrackedAnimal();var client=new TrackedAnimal();server.phase=9;server.dataTracker.set(16,(byte)(3|16|32));
        client.health=13;client.x=44;var first=AnimalRenderStateB173.capture(server);AnimalRenderStateB173.apply(client,null,first);
        assertEquals(51,client.dataTracker.getByte(16));assertEquals(9,client.phase);assertEquals(13,client.health);assertEquals(44,client.x);
        assertFalse(client.dataTracker.isDirty(),"Authoritative presentation does not create outgoing original tracker writes");
        client.dataTracker.set(16,(byte)5);server.phase=10;var second=AnimalRenderStateB173.capture(server);AnimalRenderStateB173.apply(client,first,second);
        assertEquals(5,client.dataTracker.getByte(16));assertEquals(10,client.phase);
        server.dataTracker.set(16,(byte)2);var third=AnimalRenderStateB173.capture(server);AnimalRenderStateB173.apply(client,second,third);assertEquals(2,client.dataTracker.getByte(16));
        var newLifetime=new TrackedAnimal();AnimalRenderStateB173.apply(newLifetime,null,third);assertEquals(2,newLifetime.dataTracker.getByte(16));
        assertEquals(0,new TrackedAnimal().dataTracker.getByte(16),"Another entity/lifetime has no retained tracker state");
    }
    @Test void malformedUnknownDuplicateMissingAndTypeChangedEntriesRejectBeforeAnyScalarOrTrackerMutation(){
        var server=new TrackedAnimal();server.phase=99;server.dataTracker.set(16,(byte)51);var client=new TrackedAnimal();
        var good=AnimalRenderStateB173.capture(server);
        for(int kind=0;kind<5;kind++){
            var bad=good.copy();var entries=bad.getCompound("$tracker").getList("entries",Tag.TAG_COMPOUND);var entry=entries.getCompound(0);
            switch(kind){case 0->entry.putInt("id",17);case 1->entries.add(entry.copy());case 2->entries.clear();case 3->entry.putByte("type",(byte)1);case 4->entry.putInt("value",51);}
            assertThrows(IllegalArgumentException.class,()->AnimalRenderStateB173.apply(client,null,bad));
            assertEquals(7,client.phase);assertEquals(0,client.dataTracker.getByte(16));
        }
    }
    @Test void constructorDeclarationsAndFinitePayloadBoundsRemainClosed(){
        var tracker=new DataTracker();assertThrows(IllegalArgumentException.class,()->tracker.startTracking(32,(byte)0));
        assertThrows(IllegalArgumentException.class,()->tracker.startTracking(0,1L));tracker.startTracking(0,(byte)0);
        assertThrows(IllegalArgumentException.class,()->tracker.startTracking(0,(byte)1));tracker.set(0,1);assertThrows(IllegalArgumentException.class,tracker::mcc$snapshot);
        var text=new DataTracker();text.startTracking(0,"x".repeat(65));assertThrows(IllegalArgumentException.class,text::mcc$snapshot);
        var number=new DataTracker();number.startTracking(0,Float.NaN);assertThrows(IllegalArgumentException.class,number::mcc$snapshot);
        var clean=new DataTracker();clean.startTracking(0,(byte)0);var huge=clean.mcc$snapshot();huge.putByteArray("oversized",new byte[20_000]);
        assertThrows(IllegalArgumentException.class,()->clean.mcc$prepare(null,huge));assertEquals(0,clean.getByte(0));
    }
}
