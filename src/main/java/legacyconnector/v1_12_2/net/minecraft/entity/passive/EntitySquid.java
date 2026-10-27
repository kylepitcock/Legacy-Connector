package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import static org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy;

import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.sounds.SoundEvents;

/** 1.12.2 squid: swims by its movement vector, inks when hurt. */
public class EntitySquid extends EntityWaterMob {
    public EntitySquid(World world) {
        super(world);
        setSize(0.8F, 0.8F);
    }

    protected EntitySquid(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.Squid mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.Squid m ? m : null;
    }

    public float squidPitch;
    public float prevSquidPitch;
    public float squidYaw;
    public float prevSquidYaw;
    public float squidRotation;
    public float prevSquidRotation;
    public float tentacleAngle;
    public float lastTentacleAngle;
    private float randomMotionVecX;
    private float randomMotionVecY;
    private float randomMotionVecZ;

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.7D);
    }

    /** The direction it swims in (the modern squid's movement vector for views). */
    public void setMovementVector(float x, float y, float z) {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) m.setMovementVector(x, y, z);
        randomMotionVecX = x;
        randomMotionVecY = y;
        randomMotionVecZ = z;
    }

    public boolean hasMovementVector() {
        return randomMotionVecX != 0.0F || randomMotionVecY != 0.0F || randomMotionVecZ != 0.0F;
    }

    @Override
    public float getEyeHeight() {
        return mcc$get$height(this) * 0.5F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.SQUID_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.SQUID_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.SQUID_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/squid");
    }
}
