package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class AnimalsB173Test {
    static class CustomSeat extends AnimalEntity {
        CustomSeat(World world){super(world);}
        @Override public void updatePassengerPosition(){super.updatePassengerPosition();}
        @Override public double getPassengerRidingHeight(){return super.getPassengerRidingHeight()+.5;}
    }
    static class CustomWorld extends AnimalEntity {
        CustomWorld(World world){super(world);}
        @Override public void setWorld(World world){super.setWorld(world);}
    }
    @Test void originalSeatOverridesAndSuperclassCallsAreAdmitted(){
        assertNull(AnimalsB173.unsupportedBehaviour(CustomSeat.class));
    }
    @Test void unrelatedUnimplementedOverridesStillFailAdmission(){
        assertNotNull(AnimalsB173.unsupportedBehaviour(CustomWorld.class));
    }
}
