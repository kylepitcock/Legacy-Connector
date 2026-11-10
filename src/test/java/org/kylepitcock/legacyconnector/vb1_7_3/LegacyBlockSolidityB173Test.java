package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LegacyBlockSolidityB173Test {
    @BeforeAll static void bootstrap()throws Exception{NativeStackComponentsB173Test.bootstrap();}
    private static class Fixture extends Block {
        final boolean full,empty;
        Fixture(Material material,boolean full,float height,boolean empty){super(254,material);this.full=full;this.empty=empty;setBoundingBox(0,0,0,1,height,1);}
        @Override public boolean isFullCube(){return full;}
        @Override public legacyconnector.vb1_7_3.net.minecraft.util.math.Box getCollisionShape(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z){
            return !empty&&material.blocksMovement()?getBoundingBox(world,x,y,z):null;
        }
    }
    static BlockState state(Material material,boolean full,float height,boolean occludes){
        return state(material,full,height,occludes,false);
    }
    static BlockState state(Material material,boolean full,float height,boolean occludes,boolean empty){
        Block previous=Block.BLOCKS[254];Block.BLOCKS[254]=null;
        try{var properties=BlockBehaviour.Properties.of();if(!occludes)properties.noOcclusion();if(!material.blocksMovement())properties.noCollission();
            var original=new Fixture(material,full,height,empty);
            var factory=LegacyB173Block.class.getDeclaredMethod("nativeProperties",Block.class,BlockBehaviour.Properties.class);factory.setAccessible(true);
            properties=(BlockBehaviour.Properties)factory.invoke(null,original,properties);
            var block=(LegacyB173Block)org.kylepitcock.legacyconnector.core.synth.Synth.blank(LegacyB173Block.class.getName());
            field(LegacyB173Block.class,block,"legacy",original);field(BlockBehaviour.class,block,"properties",properties);
            field(BlockBehaviour.class,block,"dynamicShape",true);
            var definition=new net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState>(block)
                    .add(LegacyB173Block.META).create(net.minecraft.world.level.block.Block::defaultBlockState,BlockState::new);
            field(net.minecraft.world.level.block.Block.class,block,"stateDefinition",definition);
            field(net.minecraft.world.level.block.Block.class,block,"defaultBlockState",definition.any().setValue(LegacyB173Block.META,0));
            var state=block.defaultBlockState();state.initCache();return state;
        }catch(ReflectiveOperationException error){throw new AssertionError(error);
        }finally{Block.BLOCKS[254]=previous;}
    }
    private static void field(Class<?> owner,Object target,String name,Object value)throws ReflectiveOperationException{
        var field=owner.getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
    @Test void dynamicFullCubeStillBlocksNativeMotionAndHeightmaps(){
        var state=state(Material.SOIL,true,1,true);
        assertTrue(state.getBlock().hasDynamicShape());assertTrue(state.isSolid());assertTrue(state.blocksMotion());
        assertTrue(state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE,BlockPos.ZERO));
        assertTrue(Heightmap.Types.MOTION_BLOCKING.isOpaque().test(state));
        assertTrue(Heightmap.Types.OCEAN_FLOOR.isOpaque().test(state));
        assertTrue(Heightmap.Types.WORLD_SURFACE.isOpaque().test(state));
    }
    @Test void solidityIsIndependentOfOpacityForMovementBlockingCubes(){
        var glass=state(Material.GLASS,true,1,false);
        assertTrue(glass.blocksMotion());assertTrue(Heightmap.Types.MOTION_BLOCKING.isOpaque().test(glass));assertFalse(glass.canOcclude());
    }
    @Test void movingPartialShapesKeepTheirCollisionsWhileNativeMaterialSolidityIsTrue(){
        var half=state(Material.STONE,true,.5F,false);
        assertTrue(half.blocksMotion());assertTrue(Heightmap.Types.MOTION_BLOCKING.isOpaque().test(half));
        assertEquals(.5,half.getCollisionShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO).bounds().maxY);
        var nonfull=state(Material.WOOD,false,1,false);assertTrue(nonfull.blocksMotion());
        var emptyIce=state(Material.ICE,false,1,false,true);
        assertTrue(emptyIce.blocksMotion(),"Movement-blocking material remains authoritative for heightmaps");
        assertTrue(emptyIce.getCollisionShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO).isEmpty(),"Original empty collision remains empty");
    }
    @Test void nonmovingMaterialsStayNonSolid(){
        for(var state:java.util.List.of(state(Material.PLANT,true,1,false),state(Material.WATER,true,1,false))){
            assertFalse(state.isSolid());assertFalse(state.blocksMotion());assertFalse(Heightmap.Types.MOTION_BLOCKING.isOpaque().test(state));
        }
    }
    @Test void ordinaryNativeControlsKeepTheirOwnSolidityAndFluidHeightPredicate(){
        assertTrue(Blocks.OAK_FENCE.defaultBlockState().blocksMotion());assertTrue(Blocks.STONE_STAIRS.defaultBlockState().blocksMotion());
        assertTrue(Blocks.CHEST.defaultBlockState().blocksMotion());assertTrue(Blocks.STONE.defaultBlockState().blocksMotion());assertTrue(Blocks.GLASS.defaultBlockState().blocksMotion());
        assertFalse(Blocks.AIR.defaultBlockState().blocksMotion());assertFalse(Blocks.SHORT_GRASS.defaultBlockState().blocksMotion());
        assertFalse(Blocks.WATER.defaultBlockState().blocksMotion());
        assertTrue(Heightmap.Types.MOTION_BLOCKING.isOpaque().test(Blocks.WATER.defaultBlockState()));
        assertFalse(Heightmap.Types.OCEAN_FLOOR.isOpaque().test(Blocks.WATER.defaultBlockState()));
    }
}
