package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.PortalBlock;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Beta portal sheets show only their two broad faces, as in Beta 1.7.3 BlockPortal.shouldSideBeRendered. */
class PortalFacesB173Test {
    private static final int ID=251;
    private static PortalBlock portal;
    @BeforeAll static void bootstrap()throws Exception {NativeStackComponentsB173Test.bootstrap();portal=new PortalBlock(ID,0);}
    @AfterAll static void release(){Block.BLOCKS[ID]=null;}

    private static BlockView cells(int[][] positions){
        Map<Long,Integer> ids=new HashMap<>();
        for(int[] p:positions)ids.put(key(p[0],p[1],p[2]),ID);
        return new BlockView(){
            public int getBlockId(int x,int y,int z){return ids.getOrDefault(key(x,y,z),0);}
            public BlockEntity getBlockEntity(int x,int y,int z){return null;}
            public float method_1782(int x,int y,int z){return 1;}
            public int getBlockMeta(int x,int y,int z){return 0;}
            public Material getMaterial(int x,int y,int z){return null;}
            public boolean method_1783(int x,int y,int z){return false;}
            public boolean shouldSuffocate(int x,int y,int z){return false;}
        };
    }
    private static long key(int x,int y,int z){return ((long)x<<40)^((long)y<<20)^(z&0xFFFFF);}

    @Test void sheetAlongXShowsNorthAndSouthFacesOnly(){
        var view=cells(new int[][]{{0,0,0},{1,0,0},{0,1,0},{1,1,0},{0,2,0},{1,2,0}});
        for(int x=0;x<=1;x++)for(int y=0;y<=2;y++)assertEquals(1<<2|1<<3,BlockFacesB173.visibleFaces(portal,view,x,y,0),x+","+y);
    }
    @Test void worldGeometryUsesPerPositionBoundsAndOriginalCulling(){
        assertTrue(BlockRenderB173.capturesWorld(portal));
        double[] before={portal.minX,portal.minY,portal.minZ,portal.maxX,portal.maxY,portal.maxZ};
        var alongX=cells(new int[][]{{0,0,0},{1,0,0},{0,1,0},{1,1,0},{0,2,0},{1,2,0}});
        var quads=BlockRenderB173.capture(portal,alongX,0,1,0);
        assertEquals(8,quads.size());
        for(var v:quads){assertTrue(v.z()==.375F||v.z()==.625F,"z "+v.z());assertTrue(v.x()>=0&&v.x()<=1&&v.y()>=1&&v.y()<=2);}
        var alongZ=cells(new int[][]{{0,0,0},{0,0,1},{0,1,0},{0,1,1},{0,2,0},{0,2,1}});
        quads=BlockRenderB173.capture(portal,alongZ,0,1,1);
        assertEquals(8,quads.size());
        for(var v:quads)assertTrue(v.x()==.375F||v.x()==.625F,"x "+v.x());
        assertArrayEquals(before,new double[]{portal.minX,portal.minY,portal.minZ,portal.maxX,portal.maxY,portal.maxZ}); // shared bounds restored
    }
    @Test void sheetAlongZShowsWestAndEastFacesOnly(){
        var view=cells(new int[][]{{0,0,0},{0,0,1},{0,1,0},{0,1,1},{0,2,0},{0,2,1}});
        for(int z=0;z<=1;z++)for(int y=0;y<=2;y++)assertEquals(1<<4|1<<5,BlockFacesB173.visibleFaces(portal,view,0,y,z),z+","+y);
    }
}
