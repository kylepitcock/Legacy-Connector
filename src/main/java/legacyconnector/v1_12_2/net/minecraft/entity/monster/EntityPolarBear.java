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

/** 1.12.2 polar bear: neutral, stands up to attack, defends its cubs. */
public class EntityPolarBear extends legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal {
    public EntityPolarBear(World world) {
        super(world);
        setSize(1.3F, 1.4F);
    }

    protected EntityPolarBear(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.PolarBear mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.PolarBear m ? m : null;
    }

    private static final DataParameter<Boolean> IS_STANDING = EntityDataManager.createKey(EntityPolarBear.class, DataSerializers.BOOLEAN);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(IS_STANDING, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(20.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
    }

    public boolean isStanding() {
        var m = mcc$self();
        return m != null ? m.isStanding() : Boolean.TRUE.equals(dataManager.get(IS_STANDING));
    }

    public void setStanding(boolean standing) {
        var m = mcc$self();
        if (m != null) m.setStanding(standing);
        else dataManager.set(IS_STANDING, standing);
    }

    public float getStandingAnimationScale(float partialTicks) {
        var m = mcc$self();
        return m != null ? m.getStandingAnimationScale(partialTicks) : isStanding() ? 1.0F : 0.0F;
    }

    @Override
    public boolean attackEntityAsMob(legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        var damage = getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        boolean hit = target != null && target.attackEntityFrom(DamageSource.causeMobDamage(this), damage == null ? 6.0F : (float) damage.getAttributeValue());
        if (hit) applyEnchantments(this, target);
        return hit;
    }

    @Override
    public boolean isBreedingItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        return false;
    }

    @Override
    public EntityPolarBear createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityPolarBear(mcc$get$world(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(isChild() ? SoundEvents.POLAR_BEAR_AMBIENT_BABY : SoundEvents.POLAR_BEAR_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.POLAR_BEAR_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.POLAR_BEAR_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/polar_bear");
    }
}
