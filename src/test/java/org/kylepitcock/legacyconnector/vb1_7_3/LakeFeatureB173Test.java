package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.world.LightType;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.gen.feature.LakeFeature;
import org.junit.jupiter.api.Test;

class LakeFeatureB173Test {
    private record Cell(int x,int y,int z) {}
    private static final class Scene extends World {
        final Map<Cell,Integer> writes=new HashMap<>();
        Material shell=Material.STONE;
        @Override public boolean isAir(int x,int y,int z){return y>64;}
        @Override public Material getMaterial(int x,int y,int z){return shell;}
        @Override public int getBlockId(int x,int y,int z){return writes.getOrDefault(new Cell(x,y,z),1);}
        @Override public int getBrightness(LightType type,int x,int y,int z){return 0;}
        @Override public boolean setBlockWithoutNotifyingNeighbors(int x,int y,int z,int id){writes.put(new Cell(x,y,z),id);return true;}
    }
    private static final class Fluid extends Block {Fluid(int id,Material material){super(id,material);}}
    @Test void upperFluidAndUnsealedBottomRejectWithoutAnyWrites(){
        var fluid=new Scene();fluid.shell=Material.WATER;
        assertFalse(new LakeFeature(9).generate(fluid,new Random(37),0,64,0));assertTrue(fluid.writes.isEmpty());
        var air=new Scene();air.shell=Material.AIR;
        assertFalse(new LakeFeature(9).generate(air,new Random(37),0,64,0));assertTrue(air.writes.isEmpty());
    }
    @Test void sealedLakeHasDeterministicLowerFluidAndUpperAir(){
        int slot=228;Block old=Block.BLOCKS[slot];boolean opaque=Block.BLOCKS_OPAQUE[slot],vision=Block.BLOCKS_ALLOW_VISION[slot],entity=Block.BLOCKS_WITH_ENTITY[slot];int opacity=Block.BLOCKS_LIGHT_OPACITY[slot];
        try{
            Block.BLOCKS[slot]=null;new Fluid(slot,Material.WATER);
            var a=new Scene();var b=new Scene();
            assertTrue(new LakeFeature(slot).generate(a,new Random(37),0,64,0));
            assertTrue(new LakeFeature(slot).generate(b,new Random(37),0,64,0));assertEquals(a.writes,b.writes);
            assertTrue(a.writes.containsValue(slot));assertTrue(a.writes.containsValue(0));
            a.writes.forEach((cell,id)->{assertTrue(cell.x>=-8&&cell.x<8&&cell.z>=-8&&cell.z<8);assertEquals(cell.y>=64?0:slot,id);});
        }finally{Block.BLOCKS[slot]=old;Block.BLOCKS_OPAQUE[slot]=opaque;Block.BLOCKS_ALLOW_VISION[slot]=vision;Block.BLOCKS_WITH_ENTITY[slot]=entity;Block.BLOCKS_LIGHT_OPACITY[slot]=opacity;}
    }
}
