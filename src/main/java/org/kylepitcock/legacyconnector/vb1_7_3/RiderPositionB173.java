package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;

/** A native mount can call back before the original mount relationship is published. */
final class RiderPositionB173 {
    private RiderPositionB173(){}
    static boolean attached(Entity original,net.minecraft.world.entity.Entity passenger){
        var host=original.mcc$modern;
        return host!=null&&passenger.getVehicle()==host&&host.getFirstPassenger()==passenger&&host.hasPassenger(passenger)
                &&original.passenger!=null&&original.passenger.mcc$modern==passenger
                &&original.passenger.vehicle==original;
    }
}
