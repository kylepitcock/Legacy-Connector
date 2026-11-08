package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VanillaTorchStatesB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    private static net.minecraft.world.level.block.state.BlockState base(int id){
        return (id==50?Blocks.TORCH:Blocks.REDSTONE_TORCH).defaultBlockState();
    }
    @Test void classicOrientationAndRedstoneLightRoundTripThroughAllNativeTorchForms(){
        Direction[] directions={Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH};
        for(int id:new int[]{50,75,76})for(int meta=1;meta<=5;meta++){
            var state=VanillaBlockStatesB173.modernState(id,meta,base(id));assertNotNull(state);
            if(meta==5)assertTrue(state.is(id==50?Blocks.TORCH:Blocks.REDSTONE_TORCH));
            else{
                assertTrue(state.is(id==50?Blocks.WALL_TORCH:Blocks.REDSTONE_WALL_TORCH));
                assertEquals(directions[meta-1],state.getValue(WallTorchBlock.FACING));
            }
            if(id!=50)assertEquals(id==76,state.getValue(RedstoneTorchBlock.LIT));
            assertEquals(meta,VanillaBlockStatesB173.legacyMeta(state).intValue());
            assertEquals(id,BlocksB173.legacyId(state));
        }
    }
    @Test void placementUsesExactWestEastNorthSouthThenFloorSupportPriority(){
        for(int id:new int[]{50,75,76}){
            for(int meta=1;meta<=5;meta++){
                final int only=meta;
                var state=VanillaTorchPlacementB173.resolve(id,0,base(id),supported->supported==only);
                assertNotNull(state);assertEquals(meta,VanillaBlockStatesB173.legacyMeta(state).intValue());
                assertEquals(id,BlocksB173.legacyId(state));
            }
            for(int first=1;first<=5;first++){
                final int minimum=first;
                var state=VanillaTorchPlacementB173.resolve(id,0,base(id),supported->supported>=minimum);
                assertEquals(first,VanillaBlockStatesB173.legacyMeta(state).intValue());
            }
            // Native placement supplies the floor predicate for an opaque block OR the classic fence.
            var fenceFloor=VanillaTorchPlacementB173.resolve(id,0,base(id),supported->supported==5);
            assertEquals(5,VanillaBlockStatesB173.legacyMeta(fenceFloor).intValue());
            assertTrue(VanillaTorchPlacementB173.resolve(id,0,base(id),supported->false).isAir());
        }
    }
    @Test void normalTorchNormalizesAnyInitialMetadataButExplicitRedstoneKeepsItsOrientation(){
        for(int meta=0;meta<16;meta++){
            var state=VanillaTorchPlacementB173.resolve(50,meta,base(50),supported->supported==2);
            assertNotNull(state);assertEquals(2,VanillaBlockStatesB173.legacyMeta(state).intValue());
        }
        for(int id:new int[]{75,76})for(int meta=1;meta<16;meta++)
            assertNull(VanillaTorchPlacementB173.resolve(id,meta,base(id),supported->fail("explicit redstone metadata must not normalize")));
    }
    @Test void soulAndUnrelatedBlocksRemainOutsideClassicTorchPolicy(){
        for(var block:List.of(Blocks.SOUL_TORCH,Blocks.SOUL_WALL_TORCH,Blocks.STONE)){
            assertNull(VanillaBlockStatesB173.modernState(50,1,block.defaultBlockState()));
            assertNull(VanillaBlockStatesB173.legacyMeta(block.defaultBlockState()));
            assertNull(VanillaTorchPlacementB173.resolve(50,0,block.defaultBlockState(),supported->fail("foreign base must not be normalized")));
        }
        for(int id:Set.of(1,51,74,77,200))
            assertNull(VanillaTorchPlacementB173.resolve(id,0,base(50),supported->fail("unsupported id must not be normalized")));
    }
}
