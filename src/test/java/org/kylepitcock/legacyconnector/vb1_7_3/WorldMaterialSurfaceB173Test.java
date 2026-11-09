package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class WorldMaterialSurfaceB173Test {
    private record Cell(int x,int y,int z) {}
    private static final class Scene extends World {
        final Map<Cell,Material> materials=new HashMap<>();
        final Map<Cell,Integer> metadata=new HashMap<>();
        Cell dirty;
        @Override public Material getMaterial(int x,int y,int z){return materials.getOrDefault(new Cell(x,y,z),Material.AIR);}
        @Override public int getBlockId(int x,int y,int z){return materials.containsKey(new Cell(x,y,z))?9:0;}
        @Override public int getBlockMeta(int x,int y,int z){return metadata.getOrDefault(new Cell(x,y,z),0);}
        @Override public void blockUpdateEvent(int x,int y,int z){dirty=new Cell(x,y,z);}
        void water(int x,int y,int z,int meta){materials.put(new Cell(x,y,z),Material.WATER);metadata.put(new Cell(x,y,z),meta);}
    }
    @Test void sourceAndFlowingSurfacesUseOriginalMetadataAndIncludeContact() {
        var scene=new Scene();scene.water(0,64,0,0);
        assertTrue(scene.isFluidInBox(Box.create(.1,64.999,.1,.9,65.1,.9),Material.WATER));
        assertFalse(scene.isFluidInBox(Box.create(.1,65.001,.1,.9,65.1,.9),Material.WATER));
        scene.water(0,64,0,6);
        assertTrue(scene.isFluidInBox(Box.create(.1,64.25,.1,.9,64.3,.9),Material.WATER));
        assertFalse(scene.isFluidInBox(Box.create(.1,64.251,.1,.9,64.3,.9),Material.WATER));
        assertFalse(scene.isFluidInBox(Box.create(.1,64,.1,.9,64.3,.9),Material.LAVA));
        assertFalse(scene.isFluidInBox(Box.create(2.1,64,2.1,2.9,64.3,2.9),Material.AIR));
    }
    @Test void fallingFluidIsFullHeightAndNegativeCoordinatesUseFloor() {
        var scene=new Scene();scene.water(-1,64,-1,12);
        assertTrue(scene.isFluidInBox(Box.create(-.9,64.99,-.9,-.1,65.01,-.1),Material.WATER));
        assertFalse(scene.isFluidInBox(Box.create(-.9,65.01,-.9,-.1,65.1,-.1),Material.WATER));
    }
    @Test void independentVerticalSlicesMeasurePartialSubmersion() {
        var scene=new Scene();scene.water(0,64,0,4);
        int wet=0;
        for(int i=0;i<5;i++)if(scene.isFluidInBox(Box.create(.1,64+i*.2,.1,.9,64+(i+1)*.2,.9),Material.WATER))wet++;
        assertEquals(3,wet);
    }
    @Test void dirtyBlockDelegatesOnlyToRedrawNotification() {
        var scene=new Scene();scene.setBlockDirty(2,65,-3);
        assertEquals(new Cell(2,65,-3),scene.dirty);
        assertTrue(scene.materials.isEmpty());assertTrue(scene.metadata.isEmpty());
    }
}
