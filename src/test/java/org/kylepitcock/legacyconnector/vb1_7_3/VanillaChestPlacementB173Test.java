package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VanillaChestPlacementB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    private static BlockState read(Map<BlockPos,BlockState> states,BlockPos p){return states.getOrDefault(p,Blocks.AIR.defaultBlockState());}
    private static void pair(Map<BlockPos,BlockState> states,BlockPos p){VanillaChestPlacementB173.placed(p,q->read(states,q),states::put);}
    @Test void bothAxesAndInsertionOrdersProduceReciprocalNativePair(){
        for(var axis:new Direction[]{Direction.EAST,Direction.SOUTH})for(boolean reverse:new boolean[]{false,true}){
            var states=new HashMap<BlockPos,BlockState>();var a=new BlockPos(0,64,0);var b=a.relative(axis);
            states.put(a,Blocks.CHEST.defaultBlockState());states.put(b,Blocks.CHEST.defaultBlockState());pair(states,reverse?b:a);
            var left=states.get(a);var right=states.get(b);
            assertNotEquals(ChestType.SINGLE,left.getValue(ChestBlock.TYPE));
            assertEquals(left.getValue(ChestBlock.TYPE).getOpposite(),right.getValue(ChestBlock.TYPE));
            assertEquals(left.getValue(ChestBlock.FACING),right.getValue(ChestBlock.FACING));
            assertEquals(b,a.relative(ChestBlock.getConnectedDirection(left)));
            assertEquals(a,b.relative(ChestBlock.getConnectedDirection(right)));
        }
    }
    @Test void thirdChestCannotExtendOrReplaceAnExistingPair(){
        var states=new HashMap<BlockPos,BlockState>();var a=new BlockPos(0,64,0);var b=a.east();var c=b.east();
        states.put(a,Blocks.CHEST.defaultBlockState());states.put(b,Blocks.CHEST.defaultBlockState());pair(states,a);
        var beforeA=states.get(a);var beforeB=states.get(b);states.put(c,Blocks.CHEST.defaultBlockState());pair(states,c);
        assertSame(beforeA,states.get(a));assertSame(beforeB,states.get(b));assertEquals(ChestType.SINGLE,states.get(c).getValue(ChestBlock.TYPE));
    }
    @Test void ambiguousClusterDoesNotCreateTripleAndUnrelatedBlocksRemainUntouched(){
        var states=new HashMap<BlockPos,BlockState>();var p=new BlockPos(0,64,0);
        for(var at:List.of(p,p.east(),p.west()))states.put(at,Blocks.CHEST.defaultBlockState());pair(states,p);
        for(var state:states.values())assertEquals(ChestType.SINGLE,state.getValue(ChestBlock.TYPE));
        states.put(p,Blocks.TRAPPED_CHEST.defaultBlockState());var before=Map.copyOf(states);pair(states,p);assertEquals(before,states);
    }
    @Test void removingPartnerRestoresSingleChest(){
        var states=new HashMap<BlockPos,BlockState>();var p=new BlockPos(0,64,0);
        states.put(p,Blocks.CHEST.defaultBlockState());states.put(p.east(),Blocks.CHEST.defaultBlockState());pair(states,p);
        states.remove(p.east());pair(states,p);assertEquals(ChestType.SINGLE,states.get(p).getValue(ChestBlock.TYPE));
    }
    @Test void chestMetadataRoundTripsWithoutDiscardingNativePairProperties(){
        for(int meta=2;meta<=5;meta++){
            var before=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,ChestType.LEFT);
            var next=VanillaBlockStatesB173.modernState(54,meta,before);
            assertEquals(meta,VanillaBlockStatesB173.legacyMeta(next));assertEquals(ChestType.LEFT,next.getValue(ChestBlock.TYPE));
            assertNull(VanillaBlockStatesB173.modernState(54,meta,Blocks.TRAPPED_CHEST.defaultBlockState()));
        }
    }
    @Test void metadataReorientsBothHalvesAndKeepsTheJoinPerpendicular(){
        var states=new HashMap<BlockPos,BlockState>();var a=new BlockPos(0,64,0);var b=a.east();
        states.put(a,Blocks.CHEST.defaultBlockState());states.put(b,Blocks.CHEST.defaultBlockState());pair(states,a);
        var next=VanillaBlockStatesB173.modernState(54,3,states.get(a));
        states.put(a,VanillaChestPlacementB173.metadata(a,states.get(a),next,p->read(states,p),states::put));
        assertEquals(Direction.SOUTH,states.get(a).getValue(ChestBlock.FACING));assertEquals(Direction.SOUTH,states.get(b).getValue(ChestBlock.FACING));
        assertEquals(b,a.relative(ChestBlock.getConnectedDirection(states.get(a))));
        var invalid=VanillaBlockStatesB173.modernState(54,5,states.get(a));
        states.put(a,VanillaChestPlacementB173.metadata(a,states.get(a),invalid,p->read(states,p),states::put));
        assertEquals(Direction.SOUTH,states.get(a).getValue(ChestBlock.FACING));assertEquals(Direction.SOUTH,states.get(b).getValue(ChestBlock.FACING));
    }
}
