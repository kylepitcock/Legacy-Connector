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

/** 1.12.2 blaze: fire immune, floats, hurt by water. */
public class EntityBlaze extends EntityMob {
    public EntityBlaze(World world) {
        super(world);
        setSize(0.6F, 1.8F);
    }

    protected EntityBlaze(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Blaze mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Blaze m ? m : null;
    }

    private static final DataParameter<Byte> ON_FIRE = EntityDataManager.createKey(EntityBlaze.class, DataSerializers.BYTE);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ON_FIRE, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23000000417232513D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48.0D);
    }

    public boolean isCharged() {
        Byte b = dataManager.get(ON_FIRE);
        return b != null && (b & 1) != 0;
    }

    public void setOnFire(boolean onFire) {
        Byte b = dataManager.get(ON_FIRE);
        byte v = b == null ? 0 : b;
        dataManager.set(ON_FIRE, (byte) (onFire ? v | 1 : v & -2));
    }

    @Override
    public boolean isImmuneToFire() {
        return true;
    }

    @Override
    public boolean isBurning() {
        return isCharged() || super.isBurning();
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.BLAZE_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.BLAZE_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.BLAZE_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/blaze");
    }
}
