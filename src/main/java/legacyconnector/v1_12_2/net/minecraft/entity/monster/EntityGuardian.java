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

/** 1.12.2 guardian: an ocean monument mob with a laser beam (a view reads the modern beam target and animations). */
public class EntityGuardian extends EntityMob {
    public EntityGuardian(World world) {
        super(world);
        setSize(0.85F, 0.85F);
    }

    protected EntityGuardian(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Guardian mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Guardian m ? m : null;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0D);
    }

    public boolean hasTargetedEntity() {
        var m = mcc$self();
        return m != null && !mcc$hosted() && m.hasActiveAttackTarget();
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase getTargetedEntity() {
        var m = mcc$self();
        if (m == null || mcc$hosted()) return null;
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(m.getActiveAttackTarget()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase l ? l : null;
    }

    public float getTailAnimation(float partialTicks) {
        var m = mcc$self();
        return m != null ? m.getTailAnimation(partialTicks) : 0.0F;
    }

    public float getSpikesAnimation(float partialTicks) {
        var m = mcc$self();
        return m != null ? m.getSpikesAnimation(partialTicks) : 0.0F;
    }

    public int getAttackDuration() {
        return 80;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.GUARDIAN_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.GUARDIAN_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.GUARDIAN_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/guardian");
    }
}
