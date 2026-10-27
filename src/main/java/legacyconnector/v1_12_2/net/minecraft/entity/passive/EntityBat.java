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

/** 1.12.2 bat (hangs upside down; a view reads the modern bat). */
public class EntityBat extends EntityAmbientCreature {
    public EntityBat(World world) {
        super(world);
        setSize(0.5F, 0.9F);
    }

    protected EntityBat(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.ambient.Bat mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.ambient.Bat m ? m : null;
    }

    private static final DataParameter<Byte> HANGING = EntityDataManager.createKey(EntityBat.class, DataSerializers.BYTE);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(HANGING, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(6.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    public boolean getIsBatHanging() {
        var m = mcc$self();
        if (m != null) return m.isResting();
        Byte b = dataManager.get(HANGING);
        return b != null && (b & 1) != 0;
    }

    public void setIsBatHanging(boolean hanging) {
        var m = mcc$self();
        if (m != null) {
            m.setResting(hanging);
            return;
        }
        dataManager.set(HANGING, (byte) (hanging ? 1 : 0));
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getIsBatHanging() && rand.nextInt(4) != 0 ? null : legacy(SoundEvents.BAT_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.BAT_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.BAT_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/bat");
    }
}
