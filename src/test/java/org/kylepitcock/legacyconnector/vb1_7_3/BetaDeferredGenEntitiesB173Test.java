package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BetaDeferredGenEntitiesB173Test {
    @BeforeAll static void bootstrap()throws Exception{BetaDeferredGenDataB173Test.bootstrap();}
    private static CompoundTag entity(UUID id,double x){
        var tag=new CompoundTag();tag.putString("id","minecraft:pig");tag.putUUID("UUID",id);
        var pos=new ListTag();pos.add(DoubleTag.valueOf(x));pos.add(DoubleTag.valueOf(70));pos.add(DoubleTag.valueOf(8));tag.put("Pos",pos);return tag;
    }
    @Test void committedNativeIdentityAndFinalOriginalNbtSurviveRestartDetachedFromInputs(){
        var data=new BetaDeferredGenData();var id=UUID.randomUUID();var nbt=entity(id,41);var original=new CompoundTag();original.putInt("dungeonX",37);nbt.put("beta",original);
        long sequence=data.recordEntity(nbt,"original.Owner",true);assertTrue(sequence>0);nbt.getCompound("beta").putInt("dungeonX",99);
        assertFalse(data.entityReady(sequence));assertTrue(data.save(new CompoundTag(),null).getList("entities",10).isEmpty());
        data.completedPopulation(Map.of());assertTrue(data.entityReady(sequence));
        var loaded=BetaDeferredGenData.load(data.save(new CompoundTag(),null),null);var entry=loaded.entityEntries().getFirst();
        assertEquals(id,entry.uuid());assertEquals(new BlockPos(41,70,8),entry.position());assertEquals("original.Owner",entry.owner());assertEquals(37,entry.nbt().getCompound("beta").getInt("dungeonX"));
        var copy=entry.nbt();copy.putString("id","changed");assertEquals("minecraft:pig",entry.nbt().getString("id"));
        loaded.acceptedEntity(sequence);assertTrue(loaded.entityEntries().isEmpty());assertTrue(loaded.save(new CompoundTag(),null).getList("entities",10).isEmpty());
    }
    @Test void entityWaitsForCommittedStructureWritesInItsTargetChunk(){
        var data=new BetaDeferredGenData();var pos=new BlockPos(41,70,8);long block=data.record(pos,Blocks.STONE.defaultBlockState(),false,true);
        long entity=data.recordEntity(entity(UUID.randomUUID(),41),"",true);data.completedPopulation(Map.of());
        assertFalse(data.entityReady(entity));data.accepted(block);assertTrue(data.entityReady(entity));
    }
    @Test void passengerChunkWaitsForItsOwnPendingStructureWrites(){
        var data=new BetaDeferredGenData();var root=entity(UUID.randomUUID(),8);var child=entity(UUID.randomUUID(),41);var passengers=new ListTag();passengers.add(child);root.put("Passengers",passengers);
        long block=data.record(new BlockPos(41,70,8),Blocks.STONE.defaultBlockState(),false,true);
        long tree=data.recordEntity(root,"",true);data.completedPopulation(Map.of());assertFalse(data.entityReady(tree));
        data.accepted(block);assertTrue(data.entityReady(tree));
    }
    @Test void transactionalBatchRejectsDuplicatesOrOversizeWithoutAllocatingRecords(){
        var data=new BetaDeferredGenData();var first=entity(UUID.randomUUID(),8);var duplicate=first.copy();
        assertFalse(data.recordEntities(List.of(new BetaDeferredGenData.EntitySnapshot(first,""),new BetaDeferredGenData.EntitySnapshot(duplicate,""))));assertTrue(data.entityEntries().isEmpty());assertEquals(0,data.save(new CompoundTag(),null).getLong("sequence"));
        var huge=first.copy();huge.putString("oversize","x".repeat(256*1024));assertEquals(-1,data.recordEntity(huge,"",false));assertTrue(data.entityEntries().isEmpty());
        var children=new ListTag();children.add(first.copy());first.put("Passengers",children);assertNull(BetaDeferredGenEntities.identities(first));
    }
    @Test void failedPopulationAbortsOnlyItsEntityRecordsAndNotBlockWrites(){
        var data=new BetaDeferredGenData();long block=data.record(BlockPos.ZERO,Blocks.STONE.defaultBlockState(),false,true);
        data.recordEntity(entity(UUID.randomUUID(),8),"",true);data.abortPopulationEntities();data.completedPopulation(Map.of());
        assertTrue(data.entityEntries().isEmpty());assertEquals(1,data.entries().size());assertTrue(data.canApply(block));
    }
    @Test void rootAcceptedPassengerRejectedRetriesOnlyMissingIdentityAndNeverAcknowledgesHiddenUuid(){
        UUID root=UUID.randomUUID(),child=UUID.randomUUID();var visible=new HashSet<UUID>();var calls=new HashMap<UUID,Integer>();
        assertFalse(BetaDeferredGenEntities.publishIdentities(List.of(root,child),visible::contains,id->{calls.merge(id,1,Integer::sum);if(id.equals(child))return false;visible.add(id);return true;}));
        assertEquals(Set.of(root),visible);
        assertTrue(BetaDeferredGenEntities.publishIdentities(List.of(root,child),visible::contains,id->{calls.merge(id,1,Integer::sum);visible.add(id);return true;}));
        assertEquals(1,calls.get(root));assertEquals(2,calls.get(child));
        assertTrue(BetaDeferredGenEntities.publishIdentities(List.of(root,child),visible::contains,id->{throw new AssertionError("Duplicate admission attempted");}));
        assertFalse(BetaDeferredGenEntities.publishIdentities(List.of(UUID.randomUUID()),id->false,id->false));
    }
    @Test void restartRetainsBoundedReplayOrderAndRejectsDuplicatePersistedSequence(){
        var data=new BetaDeferredGenData();long a=data.recordEntity(entity(UUID.randomUUID(),8),"",false),b=data.recordEntity(entity(UUID.randomUUID(),24),"",false);
        var saved=data.save(new CompoundTag(),null);var loaded=BetaDeferredGenData.load(saved,null);
        assertEquals(List.of(a),loaded.entityEntriesAfter(0,1).stream().map(BetaDeferredGenData.EntityEntry::sequence).toList());
        assertEquals(List.of(b),loaded.entityEntriesAfter(a,1).stream().map(BetaDeferredGenData.EntityEntry::sequence).toList());
        var entries=saved.getList("entities",10);entries.getCompound(1).putLong("sequence",a);assertThrows(IllegalStateException.class,()->BetaDeferredGenData.load(saved,null));
    }
    static class InitialOriginal extends legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity {
        static int shared;
        final int fixed=7;
        boolean hasDungeon;
        String label;
        legacyconnector.vb1_7_3.net.minecraft.entity.Entity transientTarget;
        int[] scratch=new int[]{1};
        InitialOriginal(){super(null);}
    }
    private static CompoundTag sidecar(InitialOriginal original){
        var tag=new CompoundTag();tag.putString("class",original.getClass().getName());tag.put("fields",AnimalRenderStateB173.captureGenerationScalars(original));return tag;
    }
    @Test void initialUnsavedOriginalScalarsSurviveWithoutOverridingNativeNbtFieldsOrReferences(){
        var source=new InitialOriginal();source.hasDungeon=true;source.label="generated";source.health=500;source.setPosition(12,70,9);source.transientTarget=new InitialOriginal();
        var initial=sidecar(source);assertEquals(2,initial.getCompound("fields").size());
        var restored=new InitialOriginal();restored.health=17;restored.setPosition(900,80,100);restored.transientTarget=new InitialOriginal();var target=restored.transientTarget;
        BetaDeferredGenEntities.prepareInitialState(restored,initial).run();
        assertTrue(restored.hasDungeon);assertEquals("generated",restored.label);assertEquals(17,restored.health);assertEquals(900,restored.x);assertEquals(80,restored.y);
        assertSame(target,restored.transientTarget);assertArrayEquals(new int[]{1},restored.scratch);assertEquals(7,restored.fixed);
        var ordinary=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();restored.writeNbt(ordinary);assertFalse(ordinary.contains("hasDungeon"));
    }
    @Test void malformedInitialScalarTypeOrClassFailsBeforeApplyingAnyOriginalField(){
        var source=new InitialOriginal();source.hasDungeon=true;source.label="generated";var initial=sidecar(source);
        var restored=new InitialOriginal();restored.label="native NBT";
        String labelKey=InitialOriginal.class.getName()+"#label";initial.getCompound("fields").putInt(labelKey,123);
        assertThrows(IllegalArgumentException.class,()->BetaDeferredGenEntities.prepareInitialState(restored,initial));assertFalse(restored.hasDungeon);assertEquals("native NBT",restored.label);
        var wrongClass=sidecar(source);wrongClass.putString("class","arbitrary.untrusted.Class");assertThrows(IllegalArgumentException.class,()->BetaDeferredGenEntities.prepareInitialState(restored,wrongClass));assertFalse(restored.hasDungeon);
    }
    @Test void scalarSidecarParticipatesInImmutableNativeNbtSizeAdmission(){
        var source=new InitialOriginal();source.label="x".repeat(256*1024);var tag=entity(UUID.randomUUID(),8);tag.put("legacyconnector:beta_initial_scalars",sidecar(source));
        var data=new BetaDeferredGenData();assertEquals(-1,data.recordEntity(tag,"",false));assertTrue(data.entityEntries().isEmpty());
    }
    @Test void reservationAdmissionIsBoundedBeforeAnyNbtRecordExists(){
        var data=new BetaDeferredGenData();for(int i=0;i<BetaDeferredGenData.MAX_ENTITIES;i++)assertTrue(data.reserveEntity());assertFalse(data.reserveEntity());assertTrue(data.entityEntries().isEmpty());
        data.releaseEntity();assertTrue(data.reserveEntity());
    }
}
