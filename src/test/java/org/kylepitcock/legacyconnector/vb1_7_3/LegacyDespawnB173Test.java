package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class LegacyDespawnB173Test {
    private static final class Habitat extends World {
        PlayerEntity closest=new PlayerEntity(this){};
        @Override public PlayerEntity getClosestPlayer(Entity entity,double distance){return closest;}
    }
    private static final class Lottery extends Random {
        int calls,result;
        @Override public int nextInt(int bound){assertEquals(800,bound);calls++;return result;}
    }
    private static final class Original extends LivingEntity {
        final Lottery lottery=new Lottery();boolean allowed=true;int guardCalls;
        Original(World world){super(world);random=lottery;}
        @Override public boolean canDespawn(){guardCalls++;return allowed;}
    }
    @Test void exactAgeAndDistanceBoundariesExecuteTheOriginalShim(){
        var world=new Habitat();var mob=new Original(world);
        world.closest.x=32;mob.despawnCounter=600;mob.mcc$tryDespawn();
        assertFalse(mob.dead);assertEquals(0,mob.lottery.calls);
        mob.despawnCounter=601;mob.lottery.result=1;mob.mcc$tryDespawn();
        assertFalse(mob.dead);assertEquals(601,mob.despawnCounter);
        mob.lottery.result=0;world.closest.x=31;mob.mcc$tryDespawn();
        assertFalse(mob.dead);assertEquals(0,mob.despawnCounter);
        mob.despawnCounter=601;world.closest.x=32;mob.mcc$tryDespawn();assertTrue(mob.dead);
    }
    @Test void hardDistanceRequiresStrictlyMoreThan128(){
        var world=new Habitat();var mob=new Original(world);world.closest.x=128;
        mob.mcc$tryDespawn();assertFalse(mob.dead);
        world.closest.x=128.001;mob.mcc$tryDespawn();assertTrue(mob.dead);
        assertEquals(0,mob.lottery.calls);
    }
    @Test void originalEligibilityAndMissingPlayerSuppressAllDistanceAndRandomActions(){
        var world=new Habitat();var mob=new Original(world);world.closest.x=256;
        mob.allowed=false;mob.despawnCounter=1000;mob.mcc$tryDespawn();assertFalse(mob.dead);
        mob.allowed=true;world.closest=null;mob.mcc$tryDespawn();assertFalse(mob.dead);
        assertEquals(2,mob.guardCalls);assertEquals(0,mob.lottery.calls);assertEquals(1000,mob.despawnCounter);
    }
}
