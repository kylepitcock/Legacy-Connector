package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.decoration.painting.*;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class PaintingsB173Test {
    static class Wall extends World {
        final Set<BlockPos> support=new HashSet<>();boolean obstructed;
        @Override public Material getMaterial(int x,int y,int z){return support.contains(new BlockPos(x,y,z))?Material.STONE:Material.AIR;}
        @Override public List<Box> getEntityCollisions(Entity e,Box b){return obstructed?List.of(b):List.of();}
    }
    @Test void originalBetaDirectionsAreNotLaterTwoDimensionalDirectionIndices(){
        var world=new Wall();world.support.add(new BlockPos(10,70,10));
        var directions=new Direction[]{Direction.NORTH,Direction.WEST,Direction.SOUTH,Direction.EAST};
        for(int facing=0;facing<4;facing++){
            var p=new PaintingEntity(world,10,70,10,facing,"Kebab");assertTrue(p.canStayAttached());assertEquals(directions[facing],PaintingsB173.direction(facing));
            assertEquals(10.5+directions[facing].getStepX()*.5625,p.x,1e-6);assertEquals(10.5+directions[facing].getStepZ()*.5625,p.z,1e-6);
        }
    }
    @Test void originalRandomSelectionUsesAllFittingBetaMotivesAndRejectsUnsupportedOrObstructedWalls(){
        var world=new Wall();assertFalse(new PaintingEntity(world,10,70,10,2,"Kebab").canStayAttached());world.support.add(new BlockPos(10,70,10));
        var p=new PaintingEntity(world,10,70,10,2);assertEquals(16,p.variant.width);assertEquals(16,p.variant.height);assertTrue(p.canStayAttached());
        assertEquals(25,PaintingVariants.values().length);assertEquals("SkullAndRoses",PaintingVariants.SKULL_AND_ROSES.id);
        assertFalse(new PaintingEntity(world,10,70,10,2,"Pointer").canStayAttached());world.obstructed=true;assertFalse(p.canStayAttached());
    }
    @Test void supportCoversEveryOriginalMotiveCellAndNbtRetainsAttachmentFacingAndMotive(){
        var world=new Wall();for(int x=9;x<=12;x++)for(int y=69;y<=72;y++)world.support.add(new BlockPos(x,y,10));
        var p=new PaintingEntity(world,10,70,10,2,"Pointer");assertTrue(p.canStayAttached());world.support.remove(new BlockPos(12,72,10));assertFalse(p.canStayAttached());
        var nbt=new NbtCompound();p.writeNbt(nbt);var restored=new PaintingEntity(world);restored.readNbt(nbt);
        assertEquals(p.variant,restored.variant);assertEquals(p.facing,restored.facing);assertEquals(p.attachmentX,restored.attachmentX);assertEquals(p.attachmentY,restored.attachmentY);assertEquals(p.attachmentZ,restored.attachmentZ);
        assertEquals(p.boundingBox.mcc$box(),restored.boundingBox.mcc$box());assertEquals("minecraft:pointer",restored.variant.mcc$id().toString());
        nbt.putString("Motive","unknown original saved motive");restored.readNbt(nbt);assertEquals(PaintingVariants.KEBAB,restored.variant);
    }
    @Test void thinBetaAttachmentVolumesRemainPositiveAtFarPositiveAndNegativeWorldCoordinates(){
        for(int coordinate:new int[]{-29_999_900,-480_000,480_000,29_999_900})for(int facing=0;facing<4;facing++){
            var world=new Wall();world.support.add(new BlockPos(coordinate,220,coordinate));
            var p=new PaintingEntity(world,coordinate,220,coordinate,facing,"Kebab");var box=p.boundingBox.mcc$box();
            assertTrue(p.canStayAttached(),"original wall support remains correct at "+coordinate+" facing "+facing);
            assertTrue(box.getXsize()>0&&box.getYsize()>0&&box.getZsize()>0,"thin painting must not collapse to a zero-volume plane");
            assertEquals(.01875,facing==0||facing==2?box.getZsize():box.getXsize(),1e-8);
            var duplicate=new PaintingEntity(world,coordinate,220,coordinate,facing,"Kebab");assertTrue(box.intersects(duplicate.boundingBox.mcc$box()),"same-wall original painting volumes overlap at far coordinates");
        }
    }
}
