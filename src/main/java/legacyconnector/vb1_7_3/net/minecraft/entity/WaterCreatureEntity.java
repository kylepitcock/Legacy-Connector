package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta water creature base: creature targeting/path callbacks, underwater breathing and collision-only spawning. */
public abstract class WaterCreatureEntity extends PathAwareEntity {
    public WaterCreatureEntity(World world){super(world);}
    @Override public boolean canBreatheInWater(){return true;}
    @Override public boolean canSpawn(){return world!=null&&world.canSpawnEntity(boundingBox);}
    @Override public int getMinAmbientSoundDelay(){return 120;}
}
