package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

class MobRidingImpulseB173Test {
    static class Rider extends AnimalEntity {
        int ticks,riding;
        Rider(){super(null);}
        @Override public void tick(){ticks++;velocityY=.125;}
        @Override public void tickRiding(){riding++;super.tickRiding();}
    }
    static class Carrier extends Entity {
        int placements;
        Carrier(){super(null);}
        @Override public void updatePassengerPosition(){placements++;super.updatePassengerPosition();}
    }
    static class Immovable extends AnimalEntity {
        Immovable(){super(null);}
        @Override public void addVelocity(double x,double y,double z){}
    }
    @Test void passengerSuperclassTicksOnceThenPlacesAndResetsLivingWalkPhase(){
        var rider=new Rider();var carrier=new Carrier();carrier.setPosition(3,40,5);rider.setVehicle(carrier);
        rider.velocityX=2;rider.velocityY=3;rider.velocityZ=4;rider.walkProgress=.75F;rider.tickRiding();
        assertEquals(1,rider.riding);assertEquals(1,rider.ticks);assertEquals(1,carrier.placements);
        assertEquals(0,rider.velocityX);assertEquals(.125,rider.velocityY);assertEquals(0,rider.velocityZ);
        assertEquals(3,rider.x);assertEquals(40+carrier.getPassengerRidingHeight()+rider.getStandingEyeHeight(),rider.y);
        assertEquals(.75F,rider.lastWalkProgress);assertEquals(0,rider.walkProgress);
    }
    @Test void rotationsWrapAt180AndDampWithTenDegreeMaximum(){
        var rider=new Rider();var carrier=new Carrier();rider.setVehicle(carrier);
        carrier.prevYaw=179;carrier.yaw=-179;carrier.prevPitch=0;carrier.pitch=80;rider.tickRiding();
        assertEquals(1,rider.yaw);assertEquals(10,rider.pitch);
        carrier.prevYaw=carrier.yaw;carrier.prevPitch=carrier.pitch;rider.tickRiding();
        assertEquals(1.5F,rider.yaw);assertEquals(20,rider.pitch);
    }
    @Test void deadVehicleStopsRidingWithoutAnotherVirtualTick(){
        var rider=new Rider();var carrier=new Carrier();rider.setVehicle(carrier);carrier.dead=true;rider.tickRiding();
        assertNull(rider.vehicle);assertEquals(0,rider.ticks);assertEquals(0,carrier.placements);
    }
    @Test void collisionImpulsesRespectOriginalNoPushOverrideAndBaseImpulseIsAdditive(){
        var mobile=new Rider();var immovable=new Immovable();mobile.setPosition(0,0,0);immovable.setPosition(1,0,0);
        mobile.onCollision(immovable);assertTrue(mobile.velocityX<0);assertEquals(0,immovable.velocityX);
        mobile.velocityX=1;mobile.velocityY=2;mobile.velocityZ=3;mobile.addVelocity(.25,-.5,2);
        assertEquals(1.25,mobile.velocityX);assertEquals(1.5,mobile.velocityY);assertEquals(5,mobile.velocityZ);
    }
    @Test void clearHandTouchesOnlyTheSelectedInventorySlotAndVanillaRemovalNotificationDoesNothing(){
        var player=new PlayerEntity(null){};var selected=new ItemStack(257,1,0);var other=new ItemStack(258,1,0);
        player.inventory.selectedSlot=2;player.inventory.main[2]=selected;player.inventory.main[3]=other;
        selected.onRemoved(player);assertSame(selected,player.inventory.main[2]);player.clearStackInHand();
        assertNull(player.inventory.main[2]);assertSame(other,player.inventory.main[3]);
    }
    @Test void originalSwingInterpolationUsesPreviousFrameAndWrapsAtOne(){
        var rider=new Rider();rider.lastSwingAnimationProgress=.9F;rider.swingAnimationProgress=.1F;
        assertEquals(1F,rider.getHandSwingProgress(.5F),1E-6);
    }
    @Test void onlyImplementedRideImpulseAndVisibilityOverridesPassFullClosureGuard(){
        assertNull(AnimalsB173.unsupportedBehaviour(Rider.class));assertNull(AnimalsB173.unsupportedBehaviour(Immovable.class));
    }
}
