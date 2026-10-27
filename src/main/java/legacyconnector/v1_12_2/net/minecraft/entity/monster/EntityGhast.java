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
}
