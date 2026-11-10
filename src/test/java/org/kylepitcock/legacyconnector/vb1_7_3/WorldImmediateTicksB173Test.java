package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import org.junit.jupiter.api.Test;

class WorldImmediateTicksB173Test {
    private static class TickBlock extends Block {
        private TickBlock(){super(250,legacyconnector.vb1_7_3.net.minecraft.block.material.Material.STONE);}
        @Override public void onTick(World w,int x,int y,int z,java.util.Random random){assertSame(w.random,random);((Fixture)w).ticks++;}
    }
    private static class Fixture extends World {
        boolean loaded=true;int id=250,queries,ticks;int[] bounds;
        @Override public boolean isRegionLoaded(int x0,int y0,int z0,int x1,int y1,int z1){bounds=new int[]{x0,y0,z0,x1,y1,z1};return loaded;}
        @Override public int getBlockId(int x,int y,int z){queries++;return id;}
    }
    @Test void immediateDispatchUsesRadiusEightCurrentIdentityAndOriginalCallback(){
        var world=new Fixture();var previous=Block.BLOCKS[250];
        var block=(Block)org.kylepitcock.legacyconnector.core.synth.Synth.blank(TickBlock.class.getName());
        Block.BLOCKS[250]=block;
        try{
            world.instantBlockUpdateEnabled=true;world.scheduleBlockUpdate(10,64,-5,250,200);
            assertEquals(1,world.ticks);assertArrayEquals(new int[]{2,56,-13,18,72,3},world.bounds);
            world.id=251;world.scheduleBlockUpdate(10,64,-5,250,1);assertEquals(1,world.ticks);
            world.loaded=false;int queries=world.queries;world.scheduleBlockUpdate(10,64,-5,250,1);
            assertEquals(queries,world.queries);assertEquals(1,world.ticks);
        }finally{Block.BLOCKS[250]=previous;}
    }
    @Test void spawnQueryStopsAtFirstAirAboveSeaLevelRatherThanHighestSolid(){
        var world=new World(){
            @Override public boolean isAir(int x,int y,int z){return y==66;}
            @Override public int getBlockId(int x,int y,int z){assertEquals(65,y);return 2;}
        };
        assertEquals(2,world.getSpawnBlockId(-3,9));
    }
    @Test void spawnQueryRetainsSeaLevelStartAndBetaCeiling(){
        var empty=new World(){@Override public boolean isAir(int x,int y,int z){return true;}
            @Override public int getBlockId(int x,int y,int z){assertEquals(63,y);return 0;}};
        assertEquals(0,empty.getSpawnBlockId(0,0));
        var full=new World(){@Override public boolean isAir(int x,int y,int z){return false;}
            @Override public int getBlockId(int x,int y,int z){assertEquals(127,y);return 1;}};
        assertEquals(1,full.getSpawnBlockId(0,0));
    }
    @Test void vanillaFluidFallbackUsesOnlyNativeVanillaIdentityAndPreservesCustomDispatch(){
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var world=new World(){@Override public void mcc$scheduleVanillaFluidTick(int x,int y,int z,int id){assertEquals(8,id);calls.incrementAndGet();}};
        var block=(Block)org.kylepitcock.legacyconnector.core.synth.Synth.blank(Block.class.getName());
        block.id=8;block.onTick(world,0,0,0,new java.util.Random());
        assertEquals(1,calls.get());
        block.id=1;block.onTick(world,0,0,0,new java.util.Random());
        block.id=8;block.mcc$owner=new Object();block.onTick(world,0,0,0,new java.util.Random());
        assertEquals(1,calls.get());
    }
}
