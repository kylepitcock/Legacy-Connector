package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockRaycastB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    static class Selectable extends Block {
        int rays;
        private Selectable(){super(254,legacyconnector.vb1_7_3.net.minecraft.block.material.Material.STONE);}
        @Override public Box getCollisionShape(World world,int x,int y,int z){return null;}
        @Override public void updateBoundingBox(BlockView world,int x,int y,int z){
            float start=world.getBlockMeta(x,y,z)==1?.6F:.1F;setBoundingBox(start,.2F,.35F,start+.2F,.8F,.65F);
        }
        @Override public HitResult raycast(World world,int x,int y,int z,Vec3d from,Vec3d to){rays++;return super.raycast(world,x,y,z,from,to);}
    }
    static class Scene extends World {
        final Map<BlockPos,BlockState> states=new HashMap<>();
        final Map<BlockPos,Integer> metadata=new HashMap<>();
        final LevelAccessor access=(LevelAccessor)Proxy.newProxyInstance(LevelAccessor.class.getClassLoader(),new Class<?>[]{LevelAccessor.class},(proxy,method,args)->switch(method.getName()){
            case "getBlockState"->states.getOrDefault(args[0],Blocks.AIR.defaultBlockState());
            case "getFluidState"->states.getOrDefault(args[0],Blocks.AIR.defaultBlockState()).getFluidState();
            case "getMinBuildHeight"->-64;case "getHeight"->384;
            default->{if(method.isDefault())yield InvocationHandler.invokeDefault(proxy,method,args);throw new UnsupportedOperationException(method.toString());}
        });
        @Override public LevelAccessor mcc$access(){return access;}
        @Override public int getBlockMeta(int x,int y,int z){return metadata.getOrDefault(new BlockPos(x,y,z),0);}
    }
    // Allocate without claiming a global legacy block ID or adding a fixture to the live content registry.
    private static Selectable selectable()throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var block=(Selectable)((sun.misc.Unsafe)field.get(null)).allocateInstance(Selectable.class);
        block.mcc$initVanilla(254,0,legacyconnector.vb1_7_3.net.minecraft.block.material.Material.STONE,1,Block.DEFAULT_SOUND_GROUP,"fixture");
        block.setBoundingBox(0,0,0,1,1,1);return block;
    }
    private static Vec3d from(){return Vec3d.create(3,64.5,.5);}
    private static Vec3d to(){return Vec3d.create(-2,64.5,.5);}
    @Test void baseSelectionCallbackIsIndependentOfCollisionBox()throws Exception {
        var block=selectable();var scene=new Scene();var hit=block.raycast(scene,0,64,0,from(),to());
        assertNotNull(hit);assertEquals(0,hit.blockX);assertEquals(64,hit.blockY);assertEquals(5,hit.side);
        assertEquals(.3,hit.pos.x,1E-6);assertEquals(1,block.rays);assertNull(block.getCollisionShape(scene,0,64,0));
    }
    @Test void worldSelectionAndExplicitCollisionFilterDifferForNonSolidBlocks(){
        var scene=new Scene();scene.states.put(new BlockPos(0,64,0),Blocks.TORCH.defaultBlockState());scene.states.put(new BlockPos(-1,64,0),Blocks.STONE.defaultBlockState());
        assertEquals(0,scene.raycast(from(),to()).blockX);assertEquals(-1,scene.raycast(from(),to(),false,true).blockX);
        scene.states.put(new BlockPos(1,64,0),Blocks.STONE.defaultBlockState());assertEquals(1,scene.raycast(from(),to()).blockX);
    }
    @Test void baseCallbackUpdatesMetadataBoundsWithoutMovingInputVectors()throws Exception {
        var block=selectable();var scene=new Scene();scene.metadata.put(new BlockPos(0,64,0),1);
        var from=from();var to=to();var hit=block.raycast(scene,0,64,0,from,to);
        assertEquals(.8,hit.pos.x,1E-6);assertEquals(.6,block.minX,1E-6);
        assertEquals(3,from.x);assertEquals(-2,to.x);
    }
    @Test void rayStartingInsideSelectionHitsTheExitFace()throws Exception {
        var hit=selectable().raycast(new Scene(),0,64,0,Vec3d.create(.2,64.5,.5),Vec3d.create(2,64.5,.5));
        assertNotNull(hit);assertEquals(5,hit.side);assertEquals(.3,hit.pos.x,1E-6);
    }
    @Test void fluidsRetainSourceOnlyPickupAndSolidOcclusion(){
        var scene=new Scene();var pos=new BlockPos(0,64,0);scene.states.put(pos,Blocks.WATER.defaultBlockState());scene.states.put(pos.west(),Blocks.STONE.defaultBlockState());
        assertEquals(-1,scene.raycast(from(),to(),false).blockX);assertEquals(0,scene.raycast(from(),to(),true).blockX);
        assertEquals(-1,scene.raycast(from(),to(),true,true).blockX);
        scene.states.put(pos,Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,1));assertEquals(-1,scene.raycast(from(),to(),true).blockX);
        scene.states.put(pos,Blocks.WATER.defaultBlockState());scene.states.put(pos.east(),Blocks.STONE.defaultBlockState());assertEquals(1,scene.raycast(from(),to(),true).blockX);
    }
    @Test void invalidSegmentsReturnNoHit(){
        var scene=new Scene();assertNull(scene.raycast(null,to()));assertNull(scene.raycast(from(),null));
        assertNull(scene.raycast(Vec3d.create(Double.NaN,64,0),to()));assertNull(scene.raycast(from(),Vec3d.create(Double.POSITIVE_INFINITY,64,0)));
    }
}
