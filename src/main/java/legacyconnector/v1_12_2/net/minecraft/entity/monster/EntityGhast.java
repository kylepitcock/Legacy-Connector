package legacyconnector.v1_12_2.net.minecraft.entity.monster;

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

/** 1.12.2 ghast: a huge Nether flyer that shoots fireballs. */
public class EntityGhast extends legacyconnector.v1_12_2.net.minecraft.entity.EntityFlying implements IMob {
    public EntityGhast(World world) {
        super(world);
        setSize(4.0F, 4.0F);
    }

    protected EntityGhast(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Ghast mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Ghast m ? m : null;
    }

    private static final DataParameter<Boolean> ATTACKING = EntityDataManager.createKey(EntityGhast.class, DataSerializers.BOOLEAN);
    private int explosionStrength = 1;

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ATTACKING, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(100.0D);
    }

    public boolean isAttacking() {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? m.isCharging() : Boolean.TRUE.equals(dataManager.get(ATTACKING));
    }

    public void setAttacking(boolean attacking) {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) m.setCharging(attacking);
        else dataManager.set(ATTACKING, attacking);
    }

    public int getFireballStrength() {
        return explosionStrength;
    }

    @Override
    public boolean isImmuneToFire() {
        return true;
    }

    @Override
    public float getEyeHeight() {
        return 2.6F;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("ExplosionPower", explosionStrength);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("ExplosionPower", 99)) explosionStrength = tag.getInteger("ExplosionPower");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.GHAST_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.GHAST_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.GHAST_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/ghast");
    }

    /** A ghast faces where it flies, or its target when that is within 64 blocks. */
    public static class AILookAround extends legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIBase {
        private final EntityGhast parentEntity;

        public AILookAround(EntityGhast ghast) {
            this.parentEntity = ghast;
            setMutexBits(2);
        }

        @Override
        public boolean shouldExecute() {
            return true;
        }

        @Override
        public void updateTask() {
            var g = parentEntity;
            var target = g.getAttackTarget();
            float yaw;
            if (target == null) {
                yaw = -((float) Math.atan2(mcc$get$motionX(g), mcc$get$motionZ(g))) * (180F / (float) Math.PI);
            } else if (target.getDistanceSq(g) < 4096.0D) {
                yaw = -((float) Math.atan2(mcc$get$posX(target) - mcc$get$posX(g), mcc$get$posZ(target) - mcc$get$posZ(g))) * (180F / (float) Math.PI);
            } else {
                return;
            }
            mcc$set$rotationYaw(g, yaw);
            mcc$set$renderYawOffset(g, yaw);
        }
    }
}
