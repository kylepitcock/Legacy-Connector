package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.dimension.PortalForcer;
import org.junit.jupiter.api.Test;

class PortalForcerB173Test {
    static class FakeWorld extends World {
        final Map<Long,Integer> blocks=new HashMap<>();int notifications,portalWrites;
        static long key(int x,int y,int z){return ((long)x&0xffffff)<<32|((long)z&0xffffff)<<8|(y&255L);}
        @Override public int getBlockId(int x,int y,int z){return y<0||y>=128?0:blocks.getOrDefault(key(x,y,z),0);}
        @Override public boolean isAir(int x,int y,int z){return getBlockId(x,y,z)==0;}
        @Override public Material getMaterial(int x,int y,int z){return getBlockId(x,y,z)==0?Material.AIR:Material.STONE;}
        @Override public boolean setBlock(int x,int y,int z,int id){
            if(id==90){assertTrue(pauseTicking);portalWrites++;}
            blocks.put(key(x,y,z),id);return true;
        }
        @Override public void notifyNeighbors(int x,int y,int z,int id){assertFalse(pauseTicking);notifications++;}
    }
    @Test void searchChoosesNearestBottomAndCentersAcrossPortalWidth(){
        var world=new FakeWorld();
        for(int y=60;y<63;y++)for(int x=-2;x<=-1;x++)world.blocks.put(FakeWorld.key(x,y,3),90);
        world.blocks.put(FakeWorld.key(40,90,40),90);
        var entity=new Entity(world){};entity.setPosition(-1.2,62,3.2);entity.yaw=37;entity.pitch=19;entity.velocityX=2;
        assertTrue(new PortalForcer().teleportToValidPortal(world,entity));
        assertEquals(-1,entity.x);assertEquals(60.5,entity.y);assertEquals(3.5,entity.z);
        assertEquals(37,entity.yaw);assertEquals(0,entity.pitch);assertEquals(0,entity.velocityX);
    }
    @Test void emptyDimensionBuildsSafeDefaultFrameAndRestoresBatchFlag(){
        var world=new FakeWorld();var entity=new Entity(world){};entity.setPosition(2,12,4);
        assertTrue(new PortalForcer().createPortal(world,entity));
        assertFalse(world.pauseTicking);assertEquals(24,world.portalWrites);assertEquals(80,world.notifications);
        assertEquals(6,world.blocks.values().stream().filter(id->id==90).count());
        assertEquals(90,world.getBlockId(2,70,4));assertEquals(49,world.getBlockId(2,69,4));
    }
    @Test void moveRetainsVirtualSearchAndCreationDispatch(){
        var calls=new java.util.ArrayList<String>();
        var placer=new PortalForcer(){
            @Override public boolean teleportToValidPortal(World world,Entity entity){calls.add("search");return calls.size()>1;}
            @Override public boolean createPortal(World world,Entity entity){calls.add("create");return true;}
        };
        placer.moveToPortal(null,null);assertEquals(java.util.List.of("search","create","search"),calls);
    }
}
