package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.world.LightType;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class WorldBrightnessB173Test {
    static class LitWorld extends World {
        int sky,block,lastY=-1,queries;
        @Override public int getBrightness(LightType type,int x,int y,int z){
            assertEquals(-17,x);assertEquals(33,z);lastY=y;queries++;
            return type==LightType.SKY?sky:block;
        }
    }
    @Test void rawCombinedLightIgnoresSkyDarknessAndRetainsStrongerBlockLight(){
        var world=new LitWorld();world.sky=15;world.block=7;world.ambientDarkness=11;
        assertEquals(15,world.getBrightness(-17,64,33));
        world.sky=3;world.block=12;
        assertEquals(12,world.getBrightness(-17,64,33));
    }
    @Test void betaHeightBoundsReturnDarkBelowWorldAndClampAboveIt(){
        var world=new LitWorld();world.sky=15;
        assertEquals(0,world.getBrightness(-17,-1,33));assertEquals(0,world.queries);
        assertEquals(15,world.getBrightness(-17,128,33));assertEquals(127,world.lastY);
        assertEquals(15,world.getBrightness(-17,Integer.MAX_VALUE,33));assertEquals(127,world.lastY);
    }
}
