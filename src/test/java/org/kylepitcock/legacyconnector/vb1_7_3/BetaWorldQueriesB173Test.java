package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.src.Loc;
import legacyconnector.vb1_7_3.net.minecraft.src.SAPI;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class BetaWorldQueriesB173Test {
    static class QueryWorld extends World {
        final Set<String> chunks=new HashSet<>();final List<String> queries=new ArrayList<>();
        final List<ItemEntity> drops=new ArrayList<>();boolean rain;int rainQueries;int rainX,rainY,rainZ;
        @Override public boolean isPosLoaded(int x,int y,int z){String key=(x>>4)+","+(z>>4);queries.add(key);return chunks.contains(key);}
        @Override public boolean isRaining(int x,int y,int z){rainQueries++;rainX=x;rainY=y;rainZ=z;return rain;}
        @Override public boolean spawnEntity(Entity entity){drops.add((ItemEntity)entity);return true;}
    }
    @Test void inclusiveRegionQueryUsesChunkPresenceAndBetaHeightBounds(){
        var world=new QueryWorld();world.chunks.addAll(List.of("-1,-1","-1,0","0,-1","0,0"));
        assertTrue(world.isRegionLoaded(-1,0,-1,0,127,0));assertEquals(4,world.queries.size());
        world.queries.clear();assertFalse(world.isRegionLoaded(-1,128,-1,0,200,0));assertTrue(world.queries.isEmpty());
        assertFalse(world.isRegionLoaded(-1,-10,-1,0,-1,0));assertTrue(world.queries.isEmpty());
        assertFalse(world.isRegionLoaded(0,64,0,16));assertTrue(world.queries.contains("-1,1"));
        world.chunks.addAll(List.of("-1,1","0,1","1,-1","1,0","1,1"));
        assertTrue(world.isRegionLoaded(0,64,0,16));
    }
    @Test void wetnessUsesCachedWaterOrRainAtFlooredLegacyCoordinates(){
        var world=new QueryWorld();var entity=new Entity(world){};entity.setPosition(-.2,65.9,-16.01);
        assertFalse(entity.isWet());assertEquals(-1,world.rainX);assertEquals(65,world.rainY);assertEquals(-17,world.rainZ);
        world.rain=true;assertTrue(entity.isWet());world.rain=false;entity.mcc$environment(true);
        int before=world.rainQueries;assertTrue(entity.isWet());assertEquals(before,world.rainQueries);
        entity.mcc$environment(false);assertFalse(entity.isWet());
    }
    @Test void sapiDropsOnePickupPerItemWithoutMutatingInputAndDoesNothingClientSide(){
        var world=new QueryWorld();world.random=new java.util.Random(173);var stack=new ItemStack(264,3,7);
        SAPI.drop(world,new Loc(-1.8,39.5,2.9),stack);assertEquals(3,world.drops.size());assertEquals(3,stack.count);
        for(var item:world.drops){
            assertNotSame(stack,item.stack);assertEquals(1,item.stack.count);assertEquals(264,item.stack.itemId);assertEquals(7,item.stack.getDamage());assertEquals(10,item.pickupDelay);
            assertTrue(item.x>=-.85&&item.x<-.15);assertTrue(item.y>=39.15&&item.y<39.85);assertTrue(item.z>=2.15&&item.z<2.85);
        }
        world.isRemote=true;SAPI.drop(world,new Loc(),stack);assertEquals(3,world.drops.size());
    }
}
