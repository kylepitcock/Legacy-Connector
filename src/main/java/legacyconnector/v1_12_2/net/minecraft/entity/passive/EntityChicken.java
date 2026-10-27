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

/** 1.12.2 chicken (lays eggs, falls slowly; a view is the modern chicken). */
public class EntityChicken extends EntityAnimal {
    public EntityChicken(World world) {
        super(world);
        setSize(0.4F, 0.7F);
    }

    protected EntityChicken(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.Chicken mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.Chicken m ? m : null;
    }

    public float wingRotation;
    public float destPos;
    public float oFlapSpeed;
    public float oFlap;
    public float wingRotDelta = 1.0F;
    public int timeUntilNextEgg;
    public boolean chickenJockey;

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIPanic(this, 1.4D));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMate(this, 1.0D));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAITempt(this, 1.0D, legacyconnector.v1_12_2.net.minecraft.init.Items.WHEAT_SEEDS, false));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIFollowParent(this, 1.1D));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(7, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(4.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!mcc$hosted() || mcc$entity == null) return;
        if (!mcc$entity.level().isClientSide() && isEntityAlive() && !isChild() && !chickenJockey && --timeUntilNextEgg <= 0) {
            playSound(legacy(SoundEvents.CHICKEN_EGG), 1.0F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
            dropItem(legacyconnector.v1_12_2.net.minecraft.init.Items.EGG, 1);
            timeUntilNextEgg = rand.nextInt(6000) + 6000;
        }
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        if (!mcc$hosted()) super.fall(distance, damageMultiplier);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        chickenJockey = tag.getBoolean("IsChickenJockey");
        if (tag.hasKey("EggLayTime")) timeUntilNextEgg = tag.getInteger("EggLayTime");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("IsChickenJockey", chickenJockey);
        tag.setInteger("EggLayTime", timeUntilNextEgg);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.CHICKEN_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.CHICKEN_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.CHICKEN_DEATH);
    }

    @Override
    public boolean isBreedingItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        if (!mcc$hosted()) return super.isBreedingItem(stack);
        return stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.WHEAT_SEEDS || stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.PUMPKIN_SEEDS || stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.MELON_SEEDS || stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.BEETROOT_SEEDS;
    }

    @Override
    public EntityChicken createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityChicken(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/chicken");
    }
}
