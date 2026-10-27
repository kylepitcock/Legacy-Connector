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

/**
 * 1.12.2 skeleton base: shoots arrows with a bow and fights in melee without one, burns in daylight. Views read the modern skeleton; a
 * mod's subclass gets the 1.12 AI, bow switching and arrows.
 */
public abstract class AbstractSkeleton extends EntityMob implements legacyconnector.v1_12_2.net.minecraft.entity.IRangedAttackMob {
    private static final DataParameter<Boolean> SWINGING_ARMS = EntityDataManager.createKey(AbstractSkeleton.class, DataSerializers.BOOLEAN);
    private legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIBase aiArrowAttack;
    private legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIBase aiAttackOnCollide;

    public AbstractSkeleton(World world) {
        super(world);
        setSize(0.6F, 1.99F);
        setCombatTask();
    }

    protected AbstractSkeleton(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SWINGING_ARMS, false);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIRestrictSun(this));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIFleeSun(this, 1.0D));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAvoidEntity(this, legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityWolf.class, 6.0F, 1.0D, 1.2D));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
        targetTasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        targetTasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    protected abstract SoundEvent getStepSound();

    /** A bow in the main hand shoots arrows, anything else fights in melee (hosted skeletons only; the modern one decides for views). */
    public void setCombatTask() {
        if (!mcc$hosted() || mcc$entity == null || mcc$entity.level().isClientSide()) return;
        if (aiAttackOnCollide == null) {
            aiAttackOnCollide = new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAttackMelee(this, 1.2D, false);
            aiArrowAttack = new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAttackRanged(this, 1.0D, 20, 15.0F);
        }
        tasks.removeTask(aiAttackOnCollide);
        tasks.removeTask(aiArrowAttack);
        if (getHeldItemMainhand().getItem() == legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId("minecraft:bow")) tasks.addTask(4, aiArrowAttack);
        else tasks.addTask(4, aiAttackOnCollide);
    }

    @Override
    public void attackEntityWithRangedAttack(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, float distanceFactor) {
        var m = mcc$entity;
        if (m == null || target == null || target.mcc$entity == null) return;
        var arrow = getArrow(distanceFactor);
        double dx = target.mcc$x() - mcc$x();
        double dy = target.getEntityBoundingBox().minY + target.mcc$get$height(target) / 3.0F - arrow.mcc$y();
        double dz = target.mcc$z() - mcc$z();
        double h = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + h * 0.20000000298023224D, dz, 1.6F, 14 - m.level().getDifficulty().getId() * 4);
        playSound(legacy(SoundEvents.SKELETON_SHOOT), 1.0F, 1.0F / (rand.nextFloat() * 0.4F + 0.8F));
        mcc$get$world(this).spawnEntity(arrow);
    }

    /** The arrow it shoots (a plain one; strays shoot slowness arrows). */
    protected legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityArrow getArrow(float distanceFactor) {
        var arrow = new legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityTippedArrow(mcc$get$world(this), this);
        arrow.setEnchantmentEffectsFromEntity(this, distanceFactor);
        return arrow;
    }

    @Override
    public void setSwingingArms(boolean swinging) {
        var m = mcc$entity;
        if (m instanceof net.minecraft.world.entity.Mob mob && !mcc$hosted()) mob.setAggressive(swinging);
        else dataManager.set(SWINGING_ARMS, swinging);
    }

    public boolean isSwingingArms() {
        var m = mcc$entity;
        if (m instanceof net.minecraft.world.entity.Mob mob && !mcc$hosted()) return mob.isAggressive();
        return Boolean.TRUE.equals(dataManager.get(SWINGING_ARMS));
    }

    @Override
    public void setItemStackToSlot(legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot slot, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        super.setItemStackToSlot(slot, stack);
        if (slot == legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.MAINHAND) setCombatTask();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setCombatTask();
    }

    @Override
    protected void setEquipmentBasedOnDifficulty(legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance difficulty) {
        super.setEquipmentBasedOnDifficulty(difficulty);
        setItemStackToSlot(legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.MAINHAND, new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId("minecraft:bow")));
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData onInitialSpawn(legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance difficulty,
            legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        if (mcc$hosted() && difficulty != null) {
            setEquipmentBasedOnDifficulty(difficulty);
            setEnchantmentBasedOnDifficulty(difficulty);
            setCombatTask();
            setCanPickUpLoot(rand.nextFloat() < 0.55F * difficulty.getClampedAdditionalDifficulty());
        }
        return data;
    }

    @Override
    public float getEyeHeight() {
        return 1.74F;
    }

    @Override
    public double getYOffset() {
        return -0.6D;
    }

    @Override
    public void onLivingUpdate() {
        if (mcc$hosted() && mcc$entity != null && !mcc$entity.level().isClientSide() && mcc$entity.level().isDay()) {
            float brightness = mcc$entity.getLightLevelDependentMagicValue();
            var pos = net.minecraft.core.BlockPos.containing(mcc$entity.getX(), mcc$entity.getEyeY(), mcc$entity.getZ());
            if (brightness > 0.5F && rand.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F && mcc$entity.level().canSeeSky(pos)
                    && getItemStackFromSlot(legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.HEAD).isEmpty()) {
                setFire(8);
            }
        }
        super.onLivingUpdate();
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute getCreatureAttribute() {
        return legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute.UNDEAD;
    }
}
