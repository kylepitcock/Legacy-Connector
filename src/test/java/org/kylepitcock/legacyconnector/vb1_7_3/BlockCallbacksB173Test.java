package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.TranslucentBlock;
import legacyconnector.vb1_7_3.net.minecraft.block.TransparentBlock;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockCallbacksB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    static class Scene implements BlockView {
        int id;boolean opaque;int x,y,z;
        @Override public int getBlockId(int x,int y,int z){this.x=x;this.y=y;this.z=z;return id;}
        @Override public boolean method_1783(int x,int y,int z){this.x=x;this.y=y;this.z=z;return opaque;}
        @Override public boolean shouldSuffocate(int x,int y,int z){return opaque;}
        @Override public int getBlockMeta(int x,int y,int z){return 3;}
        @Override public Material getMaterial(int x,int y,int z){return Material.STONE;}
        @Override public float method_1782(int x,int y,int z){return 1;}
        @Override public legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity getBlockEntity(int x,int y,int z){return null;}
    }
    static class Glass extends TranslucentBlock {Glass(){super(254,0,Material.GLASS,false);}}
    static class Leaves extends TransparentBlock {
        Leaves(){super(254,0,Material.LEAVES,false);}
        void fancy(boolean value){renderSides=value;}
    }
    static class Bounds extends Block {
        Bounds(){super(254,Material.STONE);}
        @Override public void updateBoundingBox(BlockView view,int x,int y,int z){setBoundingBox(0,0,0,1,.5F,1);}
        @Override public boolean isSideVisible(BlockView view,int x,int y,int z,int side){return super.isSideVisible(view,x,y,z,side);}
        @Override public int getPistonBehavior(){return 2;}
    }
    private static <T extends Block> T block(Class<T> type)throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        T block=type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
        block.mcc$initVanilla(254,0,Material.STONE,1,Block.DEFAULT_SOUND_GROUP,"fixture");
        block.setBoundingBox(0,0,0,1,1,1);return block;
    }
    @Test void baseSurvivalAlwaysSucceedsAndInsetFacesRemainVisibleAgainstOpaqueNeighbours()throws Exception {
        var block=block(Block.class);var scene=new Scene();scene.opaque=true;
        assertTrue(block.canGrow(null,0,0,0));
        for(int side=0;side<6;side++)assertFalse(block.isSideVisible(scene,3,4,5,side));
        block.setBoundingBox(.1F,.1F,.1F,.9F,.9F,.9F);
        for(int side=0;side<6;side++)assertTrue(block.isSideVisible(scene,3,4,5,side));
        block.setBoundingBox(0,0,0,1,1,1);scene.opaque=false;
        for(int side=0;side<6;side++)assertTrue(block.isSideVisible(scene,3,4,5,side));
    }
    @Test void breakableAndLeafBasesCullTheirOwnIdUsingOriginalFlags()throws Exception {
        var scene=new Scene();scene.id=254;var glass=block(Glass.class);var leaves=block(Leaves.class);
        assertFalse(glass.isSideVisible(scene,1,2,3,2));assertFalse(leaves.isSideVisible(scene,1,2,3,2));
        leaves.fancy(true);assertTrue(leaves.isSideVisible(scene,1,2,3,2));
        scene.id=1;assertTrue(glass.isSideVisible(scene,1,2,3,2));
        scene.opaque=true;assertFalse(glass.isSideVisible(scene,1,2,3,2));
    }
    @Test void meshFaceCaptureUsesNeighbourCoordinatesStateBoundsAndRestoresMutableBlock()throws Exception {
        var scene=new Scene();scene.opaque=true;var block=block(Bounds.class);
        assertFalse(BlockFacesB173.hasCallback(block(Block.class)));assertTrue(BlockFacesB173.hasCallback(block));
        assertEquals(1<<1,BlockFacesB173.visibleFaces(block,scene,10,20,30));
        assertEquals(11,scene.x);assertEquals(20,scene.y);assertEquals(30,scene.z);assertEquals(1,block.maxY);
        scene.opaque=false;assertEquals(63,BlockFacesB173.visibleFaces(block,scene,10,20,30));
    }
    @Test void hostedOpacityAndPistonReactionUseOriginalTablesAndBlockCallback()throws Exception {
        var block=block(Bounds.class);int previous=Block.BLOCKS_LIGHT_OPACITY[254];
        try {
            // These overrides only read the original block. Avoid constructing native registry holders after bootstrap freezes them.
            var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
            var modern=(LegacyB173Block)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(LegacyB173Block.class);
            var legacyField=LegacyB173Block.class.getDeclaredField("legacy");legacyField.setAccessible(true);legacyField.set(modern,block);
            var state=net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            var empty=net.minecraft.world.level.EmptyBlockGetter.INSTANCE;var pos=net.minecraft.core.BlockPos.ZERO;
            Block.BLOCKS_LIGHT_OPACITY[254]=1;assertEquals(1,modern.getLightBlock(state,empty,pos));
            Block.BLOCKS_LIGHT_OPACITY[254]=255;assertEquals(15,modern.getLightBlock(state,empty,pos));
            Block.BLOCKS_LIGHT_OPACITY[254]=0;assertEquals(0,modern.getLightBlock(state,empty,pos));
            assertEquals(net.minecraft.world.level.material.PushReaction.BLOCK,modern.getPistonPushReaction(state));
        }finally{Block.BLOCKS_LIGHT_OPACITY[254]=previous;}
    }
    @Test void nativePlacementNotificationsTranslateTheCurrentFluidInsteadOfOldAirOrStone() {
        var blocks=net.minecraft.world.level.block.Blocks.AIR;
        assertEquals(9,LegacyB173Block.neighborId(blocks,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()));
        assertEquals(11,LegacyB173Block.neighborId(net.minecraft.world.level.block.Blocks.STONE,net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState()));
        assertEquals(8,LegacyB173Block.neighborId(blocks,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,1)));
        assertEquals(76,LegacyB173Block.neighborId(net.minecraft.world.level.block.Blocks.REDSTONE_TORCH,blocks.defaultBlockState()));
    }
    @Test void vanillaPowerSourceViewsDelegateNativeFlagsWithoutRecursingIntoHostedBlock()throws Exception {
        var view=block(Block.class);
        for(var nativeBlock:List.of(net.minecraft.world.level.block.Blocks.LEVER,net.minecraft.world.level.block.Blocks.REDSTONE_TORCH,
                net.minecraft.world.level.block.Blocks.REDSTONE_WALL_TORCH,net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK)) {
            view.mcc$modern=nativeBlock;assertTrue(view.canEmitRedstonePower(),nativeBlock.toString());
        }
        view.mcc$modern=net.minecraft.world.level.block.Blocks.STONE;assertFalse(view.canEmitRedstonePower());
        view.mcc$modern=null;assertFalse(view.canEmitRedstonePower());
        var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        view.mcc$modern=(LegacyB173Block)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(LegacyB173Block.class);
        assertFalse(view.canEmitRedstonePower());
    }
}
