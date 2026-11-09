package legacyconnector.vb1_7_3.net.minecraft.entity.passive;

import legacyconnector.vb1_7_3.net.minecraft.entity.WaterCreatureEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta Squid identity for ordinary native views; the native entity retains its simulation and renderer. */
public class SquidEntity extends WaterCreatureEntity {
    public float tiltAngle,lastTiltAngle,rollAngle,lastRollAngle;
    public float thrustTimer,lastThrustTimer,tentacleAngle,lastTentacleAngle;

    public SquidEntity(World world){super(world);texture="/mob/squid.png";}
}
