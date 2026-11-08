package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VanillaBlockStatesB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test void betaStairsUseAllFourClassicDirectionsAndOnlyBottomHalves(){
        Direction[] facings={Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH};
        for(int id:new int[]{53,67}){
            var block=id==53?Blocks.OAK_STAIRS:Blocks.COBBLESTONE_STAIRS;
            for(int meta=0;meta<4;meta++){
                var state=VanillaBlockStatesB173.modernState(id,meta,block.defaultBlockState().setValue(StairBlock.HALF,Half.TOP));
                assertNotNull(state);assertSame(block,state.getBlock());
                assertEquals(facings[meta],state.getValue(StairBlock.FACING));
                assertEquals(Half.BOTTOM,state.getValue(StairBlock.HALF));
                assertEquals(meta,VanillaBlockStatesB173.legacyMeta(state).intValue());
                assertEquals(Half.BOTTOM,VanillaBlockStatesB173.modernState(id,meta|4,block.defaultBlockState()).getValue(StairBlock.HALF));
            }
        }
    }
    @Test void bothBetaDoorHalvesCarryFacingAndOpenBitsWithoutLaterHingeMetadata(){
        Direction[] facings={Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.NORTH};
        for(int id:new int[]{64,71}){
            var block=id==64?Blocks.OAK_DOOR:Blocks.IRON_DOOR;
            var base=block.defaultBlockState().setValue(DoorBlock.HINGE,DoorHingeSide.RIGHT).setValue(DoorBlock.POWERED,true);
            for(int meta=0;meta<16;meta++){
                var state=VanillaBlockStatesB173.modernState(id,meta,base);
                assertNotNull(state);assertSame(block,state.getBlock());
                assertEquals(facings[meta&3],state.getValue(DoorBlock.FACING));
                assertEquals((meta&8)==0?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER,state.getValue(DoorBlock.HALF));
                assertEquals((meta&4)!=0,state.getValue(DoorBlock.OPEN));
                assertEquals(DoorHingeSide.RIGHT,state.getValue(DoorBlock.HINGE));assertTrue(state.getValue(DoorBlock.POWERED));
                assertEquals(meta,VanillaBlockStatesB173.legacyMeta(state).intValue());
            }
            assertEquals(DoorHingeSide.RIGHT,base.getValue(DoorBlock.HINGE));assertTrue(base.getValue(DoorBlock.POWERED));
        }
    }
    @Test void unsupportedBlocksAndWrongBaseTypesRemainOutsideTheMapping(){
        assertNull(VanillaBlockStatesB173.modernState(1,7,Blocks.STONE.defaultBlockState()));
        assertNull(VanillaBlockStatesB173.legacyMeta(Blocks.STONE.defaultBlockState()));
        assertNull(VanillaBlockStatesB173.modernState(53,0,Blocks.BIRCH_STAIRS.defaultBlockState()));
        assertNull(VanillaBlockStatesB173.legacyMeta(Blocks.BIRCH_STAIRS.defaultBlockState()));
        assertNull(VanillaBlockStatesB173.modernState(64,0,Blocks.BIRCH_DOOR.defaultBlockState()));
        assertNull(VanillaBlockStatesB173.legacyMeta(Blocks.BIRCH_DOOR.defaultBlockState()));
        assertNull(VanillaBlockStatesB173.modernState(67,0,Blocks.OAK_STAIRS.defaultBlockState()));
        assertNull(VanillaBlockStatesB173.modernState(71,0,Blocks.OAK_DOOR.defaultBlockState()));
    }
}
