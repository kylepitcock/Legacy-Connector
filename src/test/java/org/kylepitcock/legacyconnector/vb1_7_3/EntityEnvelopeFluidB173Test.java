package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.nbt.*;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EntityEnvelopeFluidB173Test {
    @BeforeAll static void bootstrap() throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    private static class View extends Entity {
        int writes,reads,custom=73;boolean sawRestoredPosition;
        View(World world){super(world);}
        @Override public float getEyeHeight(){return .2F;}
        @Override public void writeNbt(NbtCompound tag){writes++;super.writeNbt(tag);tag.putInt("Custom",custom);}
        @Override public void readNbt(NbtCompound tag){reads++;sawRestoredPosition=x==2.5;super.readNbt(tag);custom=tag.getInt("Custom");}
    }
    private static class NativeEye extends net.minecraft.world.entity.Entity {
        boolean water,lava;
        NativeEye(){super(net.minecraft.world.entity.EntityType.PIG,null);}
        @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){}
        @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){}
        @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){}
        @Override public boolean isEyeInFluid(net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> tag){return tag==net.minecraft.tags.FluidTags.WATER?water:tag==net.minecraft.tags.FluidTags.LAVA&&lava;}
    }
    private static class FluidWorld extends World {
        int meta=4;
        @Override public int getBlockId(int x,int y,int z){return 9;}
        @Override public int getBlockMeta(int x,int y,int z){return meta;}
        @Override public Material getMaterial(int x,int y,int z){return Material.WATER;}
    }
    @Test void nativeEyeQueryDoesNotSubstituteBodyWetnessAndRetainsVirtualOverrides() throws Exception {
        // Only the overridden eye query is used; no world or NeoForge fluid registry is needed.
        var unsafe=Class.forName("sun.misc.Unsafe");var field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
        var nativeEye=(NativeEye)unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),NativeEye.class);
        var view=new View(null);view.mcc$modern=nativeEye;view.mcc$environment(true);
        assertTrue(view.isWet());assertFalse(view.isInFluid(Material.WATER));nativeEye.water=true;
        assertTrue(view.isInFluid(Material.WATER));assertFalse(view.isInFluid(Material.LAVA));assertFalse(view.isInFluid(Material.STONE));
        var custom=new View(null){@Override public boolean isInFluid(Material material){return false;}};custom.mcc$modern=nativeEye;assertFalse(custom.isInFluid(Material.WATER));
    }
    @Test void standaloneEyeUsesFluidSurfaceMetadataRatherThanBodyContact(){
        var world=new FluidWorld();var view=new View(world);view.setPosition(-.5,64.3,-.5);
        assertTrue(view.isInFluid(Material.WATER));view.setPosition(-.5,64.36,-.5);assertFalse(view.isInFluid(Material.WATER));
        world.meta=8;assertTrue(view.isInFluid(Material.WATER));assertFalse(view.isInFluid(Material.LAVA));
    }
    @Test void fullEnvelopeDispatchesCustomHooksAndRetainsTypedMotionRotationAndCameraOffset(){
        var source=new View(null);source.setPosition(2.5,64,-7);source.cameraOffset=.25F;source.velocityX=10;source.velocityY=-10.01;source.velocityZ=2;
        source.yaw=25;source.pitch=-12;source.air=87;source.fireTicks=11;source.onGround=true;source.mcc$fallDistance(3.5F);
        var tag=new NbtCompound();source.write(tag);assertEquals(1,source.writes);assertEquals(6,tag.getList("Pos").mcc$elementType());assertEquals(5,tag.getList("Rotation").mcc$elementType());
        var target=new View(null);target.read(tag);assertEquals(1,target.reads);assertTrue(target.sawRestoredPosition);assertEquals(73,target.custom);
        assertEquals(2.5,target.x);assertEquals(64.25,target.y);assertEquals(target.y,target.prevY);assertEquals(target.y,target.lastTickY);
        assertEquals(10,target.velocityX);assertEquals(0,target.velocityY);assertEquals(2,target.velocityZ);assertEquals(25,target.yaw);assertEquals(-12,target.prevPitch);
        assertEquals(87,target.air);assertEquals(11,target.fireTicks);assertTrue(target.onGround);assertEquals(3.5F,target.mcc$fallDistance());
    }
    @Test void playerReadRebuildsBoundsWithoutAddingEyeOffsetTwice(){
        var player=new PlayerEntity(null){};player.setPosition(3,65.62,4);var tag=new NbtCompound();player.write(tag);
        player.setPosition(0,0,0);player.read(tag);assertEquals(65.62,player.y,1E-10);assertEquals(player.y-player.standingEyeHeight,player.boundingBox.minY,1E-10);
    }
}
