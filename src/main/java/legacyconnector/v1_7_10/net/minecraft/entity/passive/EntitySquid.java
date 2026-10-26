package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A squid. A plain one is the modern squid; mods' subclasses swim as water mobs. */
public class EntitySquid extends EntityWaterMob {
    public float squidPitch;
    public float prevSquidPitch;
    public float squidYaw;
    public float prevSquidYaw;
    public float squidRotation;
    public float prevSquidRotation;
    public float tentacleAngle;
    public float lastTentacleAngle;

    /** View of a modern squid. */
    public EntitySquid(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntitySquid(World world) {
        super(world);
        setSize(0.95F, 0.95F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(10.0D);
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public boolean getCanSpawnHere() {
        return mcc$get$posY(this) > 45.0D && mcc$get$posY(this) < 63.0D && super.getCanSpawnHere();
    }
}
