package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BetaDeferredGenDataB173Test {
    @BeforeAll static void bootstrap() throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test void restartCodecPreservesFullStatesPositionsAndPerPositionWriteOrder(){
        var data=new BetaDeferredGenData();var pos=new BlockPos(-171,39,118);
        var firstState=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
        long first=data.record(pos,firstState,false),second=data.record(pos,Blocks.CHEST.defaultBlockState(),false);
        var other=new BlockPos(172,39,134);long third=data.record(other,Blocks.OBSIDIAN.defaultBlockState(),false);
        var restored=BetaDeferredGenData.load(data.save(new CompoundTag(),null),null);
        assertEquals(List.of(first,second,third),restored.entries().stream().map(BetaDeferredGenData.Entry::sequence).toList());
        assertSame(firstState,restored.entries().getFirst().state());assertEquals(pos,restored.entries().getFirst().position());
        assertTrue(restored.canApply(first));assertFalse(restored.canApply(second));assertTrue(restored.canApply(third));
        restored.accepted(first);assertTrue(restored.canApply(second));
        var savedAgain=BetaDeferredGenData.load(restored.save(new CompoundTag(),null),null);
        assertEquals(List.of(second,third),savedAgain.entries().stream().map(BetaDeferredGenData.Entry::sequence).toList());
        assertTrue(savedAgain.record(BlockPos.ZERO,Blocks.STONE.defaultBlockState(),false)>third);
    }
    @Test void finalTileLootSurvivesUntilAttachmentIsAcceptedAndSnapshotsAreDetached(){
        var data=new BetaDeferredGenData();var pos=new BlockPos(171,39,118);var state=Blocks.CHEST.defaultBlockState();
        long block=data.record(pos,state,false),attachment=data.record(pos,state,true);
        var chest=new legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity();chest.x=pos.getX();chest.y=pos.getY();chest.z=pos.getZ();
        chest.inventory[3]=new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(264,4,0);
        var originalTag=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();chest.writeNbt(originalTag);
        var tile=NbtB173.toModern(originalTag);data.tile(pos,tile);tile.putString("id","changed input");
        data.accepted(block);assertTrue(data.contains(pos));assertEquals("Chest",data.tile(pos).getString("id"));
        var restored=BetaDeferredGenData.load(data.save(new CompoundTag(),null),null);
        assertTrue(restored.entries().getFirst().attachment());assertTrue(restored.canApply(attachment));
        var read=restored.tile(pos);read.putString("id","changed output");assertEquals("Chest",restored.tile(pos).getString("id"));
        var loaded=assertInstanceOf(legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity.class,
                legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity.createFromNbt(NbtB173.fromModern(restored.tile(pos))));
        assertEquals(264,loaded.inventory[3].itemId);assertEquals(4,loaded.inventory[3].count);assertEquals(pos.getX(),loaded.x);
        restored.accepted(attachment);assertFalse(restored.contains(pos));assertNull(restored.tile(pos));assertTrue(restored.entries().isEmpty());
    }
    @Test void unfinishedPopulationIsAbsentFromDiskAndCommitMarksDataDirty(){
        var data=new BetaDeferredGenData();var pos=new BlockPos(2,39,4);var state=Blocks.CHEST.defaultBlockState();
        long block=data.record(pos,state,false,true),attachment=data.record(pos,state,true,true);
        assertFalse(data.canApply(block));assertEquals(List.of(pos),data.populationPositions());
        var before=data.save(new CompoundTag(),null);assertTrue(before.getList("pending",10).isEmpty());assertTrue(before.getList("tiles",10).isEmpty());
        data.setDirty(false);var tile=new CompoundTag();tile.putString("id","Chest");tile.putInt("final_loot",4);
        data.completedPopulation(Map.of(pos,tile));tile.putInt("final_loot",99);
        assertTrue(data.isDirty());assertTrue(data.canApply(block));assertFalse(data.canApply(attachment));
        var restored=BetaDeferredGenData.load(data.save(new CompoundTag(),null),null);
        assertEquals(List.of(block,attachment),restored.entries().stream().map(BetaDeferredGenData.Entry::sequence).toList());
        assertEquals(4,restored.tile(pos).getInt("final_loot"));assertTrue(data.populationPositions().isEmpty());
    }
    @Test void populationSnapshotsAreLimitedToTheirOwningThread(){
        var data=new BetaDeferredGenData();var first=new BlockPos(2,39,4);var second=new BlockPos(20,39,40);
        data.record(first,Blocks.STONE.defaultBlockState(),false,true);
        java.util.concurrent.CompletableFuture.runAsync(()->{
            data.record(second,Blocks.CHEST.defaultBlockState(),true,true);
            assertEquals(List.of(second),data.populationPositions());
            var tile=new CompoundTag();tile.putString("id","Chest");data.completedPopulation(Map.of(second,tile));
        }).join();
        assertEquals(List.of(first),data.populationPositions());
        var restored=BetaDeferredGenData.load(data.save(new CompoundTag(),null),null);
        assertEquals(List.of(second),restored.entries().stream().map(BetaDeferredGenData.Entry::position).toList());
        assertEquals("Chest",restored.tile(second).getString("id"));
        data.completedPopulation(Map.of());
    }
    @Test void admissionLimitRejectsBeforeCreatingMarkedRecordsOrOverlays() throws Exception {
        var type=org.kylepitcock.legacyconnector.v1_7_10.bridge.DeferredGenWrites.class;
        var countField=type.getDeclaredField("COUNT");countField.setAccessible(true);
        var maxField=type.getDeclaredField("MAX");maxField.setAccessible(true);int max=maxField.getInt(null);
        var count=(java.util.concurrent.atomic.AtomicInteger)countField.get(null);int previous=count.get();
        var reserve=type.getDeclaredMethod("reserveIntercepted");reserve.setAccessible(true);
        try{
            count.set(max-1);assertEquals(true,reserve.invoke(null));assertEquals(max,count.get());assertEquals(false,reserve.invoke(null));
            assertDoesNotThrow(()->org.kylepitcock.legacyconnector.v1_7_10.bridge.DeferredGenWrites.interceptedBlock(null,BlockPos.ZERO,Blocks.STONE.defaultBlockState()));
            assertThrows(IllegalStateException.class,()->org.kylepitcock.legacyconnector.v1_7_10.bridge.DeferredGenWrites.restoreIntercepted(null,1,BlockPos.ZERO,Blocks.STONE.defaultBlockState(),false,null));
            assertEquals(max,count.get());
        }finally{count.set(previous);}
    }
}
