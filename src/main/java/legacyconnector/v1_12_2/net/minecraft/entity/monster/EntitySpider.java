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

/** 1.12.2 spider: climbs walls (a view reads the modern spider), leaps at its target. */
public class EntitySpider extends EntityMob {
    public EntitySpider(World world) {
        super(world);
        setSize(1.4F, 0.9F);
    }

    protected EntitySpider(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Spider mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Spider m ? m : null;
    }

    private static final DataParameter<Byte> CLIMBING = EntityDataManager.createKey(EntitySpider.class, DataSerializers.BYTE);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(CLIMBING, (byte) 0);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILeapAtTarget(this, 0.4F));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAttackMelee(this, 1.0D, true));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 0.8D));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
        targetTasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        targetTasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(16.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
    }

    public boolean isBesideClimbableBlock() {
        var m = mcc$self();
        if (m != null) return m.isClimbing();
        Byte b = dataManager.get(CLIMBING);
        return b != null && (b & 1) != 0;
    }

    public void setBesideClimbableBlock(boolean climbing) {
        var m = mcc$self();
        if (m != null) {
            m.setClimbing(climbing);
            return;
        }
        Byte b = dataManager.get(CLIMBING);
        byte v = b == null ? 0 : b;
        dataManager.set(CLIMBING, (byte) (climbing ? v | 1 : v & -2));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (mcc$hosted() && mcc$entity != null && !mcc$entity.level().isClientSide()) setBesideClimbableBlock(mcc$get$collidedHorizontally(this));
    }

    @Override
    public boolean isOnLadder() {
        return isBesideClimbableBlock();
    }

    @Override
    public void setInWeb() {
    }

    @Override
    public boolean isPotionApplicable(legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect effect) {
        return effect.getPotion() != legacyconnector.v1_12_2.net.minecraft.init.MobEffects.POISON && super.isPotionApplicable(effect);
    }

    @Override
    public double getMountedYOffset() {
        return mcc$get$height(this) * 0.5F;
    }

    @Override
    public float getEyeHeight() {
        return 0.65F;
    }

    protected SoundEvent getStepSound() {
        return legacy(SoundEvents.SPIDER_STEP);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.SPIDER_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.SPIDER_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.SPIDER_DEATH);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute getCreatureAttribute() {
        return legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute.ARTHROPOD;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/spider");
    }
}
