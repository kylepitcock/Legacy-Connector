package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult;
import legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResultType;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

class BoxRaycastB173Test {
    private static final Box UNIT=Box.create(0,0,0,1,1,1);
    private static Vec3d point(double x,double y,double z){return Vec3d.create(x,y,z);}
    private static void hit(Box box,Vec3d from,Vec3d to,int side,Vec3d expected){
        HitResult hit=box.raycast(from,to);assertNotNull(hit);assertEquals(side,hit.side);assertEquals(HitResultType.BLOCK,hit.type);
        assertEquals(0,hit.blockX);assertEquals(0,hit.blockY);assertEquals(0,hit.blockZ);
        assertEquals(expected.x,hit.pos.x,1E-12);assertEquals(expected.y,hit.pos.y,1E-12);assertEquals(expected.z,hit.pos.z,1E-12);
    }
    @Test void sixEntryAndInsideExitFacesKeepBetaSideIds(){
        Vec3d center=point(.5,.5,.5);
        Vec3d[] outside={point(-1,.5,.5),point(2,.5,.5),point(.5,-1,.5),point(.5,2,.5),point(.5,.5,-1),point(.5,.5,2)};
        Vec3d[] faces={point(0,.5,.5),point(1,.5,.5),point(.5,0,.5),point(.5,1,.5),point(.5,.5,0),point(.5,.5,1)};
        int[] ids={4,5,0,1,2,3};
        for(int i=0;i<outside.length;i++){hit(UNIT,outside[i],center,ids[i],faces[i]);hit(UNIT,center,outside[i],ids[i],faces[i]);}
    }
    @Test void insideShrinkSegmentHitsExitAfterOriginalItemSuperclassMovement(){
        hit(Box.create(-1.05,0,-1.05,1.05,1.5,1.05),point(0,.46,.5),point(0,.4208,2.46),3,point(0,.449,1.05));
    }
    @Test void containedSegmentStationarySegmentAndOutsideMissHaveNoFaceIntersection(){
        assertNull(UNIT.raycast(point(.2,.3,.4),point(.8,.7,.6)));
        assertNull(UNIT.raycast(point(.5,.5,.5),point(.5,.5,.5)));
        assertNull(UNIT.raycast(point(-1,2,.5),point(2,2,.5)));
        assertNull(UNIT.raycast(point(-2,.5,.5),point(-1,.5,.5)));
    }
    @Test void parallelFacesAndTangentEdgesUseInclusiveOtherCoordinates(){
        hit(UNIT,point(-1,0,.5),point(2,0,.5),4,point(0,0,.5));
        hit(UNIT,point(-1,1,1),point(2,1,1),4,point(0,1,1));
        hit(UNIT,point(-1,1,.5),point(1,-1,.5),4,point(0,0,.5));
        assertNull(UNIT.raycast(point(-1,1.00001,.5),point(2,1.00001,.5)));
    }
    @Test void boundaryStartsAndEndpointsAreHitsEvenWhenMovingOutward(){
        hit(UNIT,point(0,.5,.5),point(-1,.5,.5),4,point(0,.5,.5));
        hit(UNIT,point(1,.5,.5),point(2,.5,.5),5,point(1,.5,.5));
        hit(UNIT,point(-1,.5,.5),point(0,.5,.5),4,point(0,.5,.5));
    }
    @Test void tiesUseOriginalXThenYThenZFacePrecedence(){
        hit(UNIT,point(-1,-1,-1),point(2,2,2),4,point(0,0,0));
        hit(UNIT,point(.5,.5,.5),point(2,2,2),5,point(1,1,1));
        hit(UNIT,point(.5,-1,-1),point(.5,2,2),0,point(.5,0,0));
    }
    @Test void nearParallelAxisUsesOriginalInterpolationEpsilon(){
        assertNull(UNIT.raycast(point(-.0001,.5,.5),point(.0001,.5,.5)));
        hit(UNIT,point(-.001,.5,.5),point(.001,.5,.5),4,point(0,.5,.5));
    }
}
