package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.SleepAttemptResult;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class BedsB173Test {
    private static PlayerEntity detached(){return new PlayerEntity(null){};}
    @Test void ordinaryAwakeCallbacksPreserveCustomizedEyeOffset(){
        var player=detached();player.standingEyeHeight=.9F;
        player.mcc$syncSleeping(false);player.mcc$syncSleeping(false);
        assertEquals(.9F,player.standingEyeHeight);
    }
    @Test void directOriginalSleepRestoresPresleepOffsetOnExplicitAndNativeWake(){
        for(float offset:new float[]{.9F,1.62F}){
            var player=detached();player.standingEyeHeight=offset;player.mcc$syncSleeping(false);
            player.standingEyeHeight=.2F;player.sleeping=true;
            player.wakeUp(true,false,false);assertEquals(offset,player.standingEyeHeight);
            player.mcc$syncSleeping(false);player.standingEyeHeight=.2F;player.sleeping=true;
            player.mcc$syncSleeping(false);assertEquals(offset,player.standingEyeHeight);
        }
    }
    @Test void firstNativeSleepingMirrorRetainsItsPreviousAwakeOffset(){
        var player=detached();player.standingEyeHeight=.9F;
        player.mcc$syncSleeping(true);assertEquals(.2F,player.standingEyeHeight);
        player.mcc$syncSleeping(false);assertEquals(.9F,player.standingEyeHeight);
    }
    @Test void originalTimerThresholdAndWakeFlagsRemainDistinct(){
        var player=detached();player.sleeping=true;player.sleepTimer=99;
        assertTrue(player.isSleeping());assertFalse(player.isFullyAsleep());
        player.sleepTimer=100;assertTrue(player.isFullyAsleep());
        player.wakeUp(false,false,false);assertFalse(player.isSleeping());assertEquals(100,player.getSleepTimer());
        player.wakeUp(true,false,false);assertEquals(0,player.getSleepTimer());assertEquals(1.62F,player.standingEyeHeight);
    }
    @Test void directFieldSleepPlacementFindsTheOriginalBedIncludingNegativeCoordinates(){
        var player=detached();player.x=-10.9;player.y=63.9375;player.z=-2.1;
        assertEquals(new BlockPos(-11,63,-3),BedsB173.requestedPosition(player));
        player.sleepingPos=new Vec3i(7,8,9);assertEquals(new BlockPos(7,8,9),BedsB173.requestedPosition(player));
    }
    @Test void allDirectionsAndOccupancyRetainTheHeadAndDirectionBits(){
        Direction[] directions={Direction.SOUTH,Direction.WEST,Direction.NORTH,Direction.EAST};
        for(int meta=0;meta<16;meta++){
            assertEquals(directions[meta&3],BedsB173.direction(meta));
            int occupied=BedsB173.occupied(meta,true);assertEquals(meta&11,occupied&11);assertEquals(4,occupied&4);
            assertEquals(meta&11,BedsB173.occupied(occupied,false));
            assertEquals(meta&3,legacyconnector.vb1_7_3.net.minecraft.block.BedBlock.getDirection(meta));
        }
    }
    @Test void originalSleepResultIsAnActualEnumAndDetachedTrySleepDoesNotInventSuccess(){
        assertEquals(5,SleepAttemptResult.values().length);
        assertSame(SleepAttemptResult.OTHER_PROBLEM,detached().trySleep(0,0,0));
    }
    @Test void concreteOriginalSleepSignaturesAreAvailable() throws Exception {
        assertEquals(SleepAttemptResult.class,PlayerEntity.class.getMethod("trySleep",int.class,int.class,int.class).getReturnType());
        PlayerEntity.class.getMethod("wakeUp",boolean.class,boolean.class,boolean.class);
        legacyconnector.vb1_7_3.net.minecraft.world.World.class.getMethod("updateSleepingPlayers");
        assertEquals(boolean.class,PlayerEntity.class.getField("sleeping").getType());
        assertEquals(Vec3i.class,PlayerEntity.class.getField("sleepingPos").getType());
    }
}
