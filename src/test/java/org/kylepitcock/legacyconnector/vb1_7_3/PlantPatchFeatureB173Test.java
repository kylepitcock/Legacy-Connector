package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.PlantBlock;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.gen.feature.PlantPatchFeature;
import org.junit.jupiter.api.Test;

class PlantPatchFeatureB173Test {
    static class Ground extends World {
        int soil=3,light=8,writes;boolean sky,air=true;
        @Override public int getBlockId(int x,int y,int z){return y==63?soil:0;}
        @Override public int getBrightness(int x,int y,int z){return light;}
        @Override public boolean hasSkyLight(int x,int y,int z){return sky;}
        @Override public boolean isAir(int x,int y,int z){return air;}
        @Override public boolean setBlockWithoutNotifyingNeighbors(int x,int y,int z,int id){assertEquals(64,y);writes++;return true;}
        @Override public boolean setBlock(int x,int y,int z,int id){fail("Patch must not notify neighbors");return false;}
    }
    static class Plant extends PlantBlock {Plant(int id){super(id,0);}}
    private static int freeSlot(){for(int id=255;id>96;id--)if(Block.BLOCKS[id]==null)return id;throw new AssertionError("No fixture block slot");}
    @Test void defaultPlantSurvivalRequiresOriginalSoilAndRawLightOrSky(){
        int id=freeSlot();var plant=new Plant(id);var world=new Ground();
        try{
            assertTrue(plant.canGrow(world,0,64,0));world.light=7;assertFalse(plant.canGrow(world,0,64,0));
            world.sky=true;assertTrue(plant.canGrow(world,0,64,0));world.soil=1;assertFalse(plant.canGrow(world,0,64,0));
        }finally{Block.BLOCKS[id]=null;}
    }
    @Test void patchExecutes64AttemptsAndRespectsAirAndVirtualSurvival(){
        int id=freeSlot();int[] checked={0};var plant=new Plant(id){@Override public boolean canGrow(World world,int x,int y,int z){checked[0]++;return true;}};
        var world=new Ground();int[] samples={0};
        var random=new java.util.Random(){@Override public int nextInt(int bound){assertTrue(bound==8||bound==4);samples[0]++;return 0;}};
        try{
            var feature=new PlantPatchFeature(id);assertTrue(feature.generate(world,random,0,64,0));
            assertEquals(64,world.writes);assertEquals(64,checked[0]);assertEquals(384,samples[0]);
            world.air=false;feature.generate(world,random,0,64,0);assertEquals(64,world.writes);assertEquals(64,checked[0]);
        }finally{Block.BLOCKS[id]=null;}
    }
    @Test void detachedVanillaMushroomsUseOriginalHeightLightAndOpaqueSupport(){
        int slot=freeSlot();var plant=new Plant(slot);plant.id=39;var world=new Ground();
        boolean previous=Block.BLOCKS_OPAQUE[1];Block.BLOCKS_OPAQUE[1]=true;
        try{
            world.soil=1;world.light=12;assertTrue(plant.canGrow(world,0,64,0));
            world.light=13;assertFalse(plant.canGrow(world,0,64,0));world.light=0;
            assertFalse(plant.canGrow(world,0,-1,0));assertFalse(plant.canGrow(world,0,128,0));
            world.soil=0;assertFalse(plant.canGrow(world,0,64,0));
        }finally{Block.BLOCKS[slot]=null;Block.BLOCKS_OPAQUE[1]=previous;}
    }
}
