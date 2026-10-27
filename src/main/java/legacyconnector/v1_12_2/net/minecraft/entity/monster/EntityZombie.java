package legacyconnector.v1_12_2.net.minecraft.entity.monster;

import static org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy;

import legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData;
import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIZombieAttack;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.RangedAttribute;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.sounds.SoundEvents;

/**
 * 1.12.2 zombie. A view of a modern zombie reads and changes the modern one (child, arms raised); a mod's subclass is a hosted mob
 * with the 1.12 zombie AI, attributes, sounds and loot.
 */
public class EntityZombie extends EntityMob {
    public static final IAttribute SPAWN_REINFORCEMENTS_CHANCE = new RangedAttribute(null, "zombie.spawnReinforcements", 0.0D, 0.0D, 1.0D)
            .setDescription("Spawn Reinforcements Chance");
    private static final DataParameter<Boolean> IS_CHILD = EntityDataManager.createKey(EntityZombie.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> VILLAGER_TYPE = EntityDataManager.createKey(EntityZombie.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> ARMS_RAISED = EntityDataManager.createKey(EntityZombie.class, DataSerializers.BOOLEAN);
    private boolean mcc$canBreakDoors;

    public EntityZombie(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    protected EntityZombie(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Zombie mcc$zombie() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Zombie z ? z : null;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(IS_CHILD, false);
        dataManager.register(VILLAGER_TYPE, 0);
        dataManager.register(ARMS_RAISED, false);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIZombieAttack(this, 1.0D, false));
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D));
        tasks.addTask(7, new EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        applyEntityAI();
    }

    protected void applyEntityAI() {
        tasks.addTask(6, new EntityAIMoveThroughVillage(this, 1.0D, false));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, EntityPigZombie.class));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager.class, false));
        targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(35.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(2.0D);
        getAttributeMap().registerAttribute(SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(rand.nextDouble() * 0.1D);
    }

    public void setArmsRaised(boolean raised) {
        var z = mcc$zombie();
        if (z != null) z.setAggressive(raised);
        else dataManager.set(ARMS_RAISED, raised);
    }

    public boolean isArmsRaised() {
        var z = mcc$zombie();
        return z != null ? z.isAggressive() : Boolean.TRUE.equals(dataManager.get(ARMS_RAISED));
    }

    public boolean isBreakDoorsTaskSet() {
        var z = mcc$zombie();
        return z != null ? z.canBreakDoors() : mcc$canBreakDoors;
    }

    public void setBreakDoorsAItask(boolean enabled) {
        var z = mcc$zombie();
        if (z != null) z.setCanBreakDoors(enabled);
        else mcc$canBreakDoors = enabled;
    }

    @Override
    public boolean isChild() {
        var z = mcc$zombie();
        return z != null ? z.isBaby() : Boolean.TRUE.equals(dataManager.get(IS_CHILD));
    }

    public void setChild(boolean child) {
        var z = mcc$zombie();
        if (z != null) {
            z.setBaby(child);
            return;
        }
        dataManager.set(IS_CHILD, child);
        setChildSize(child);
    }

    public void setChildSize(boolean child) {
        multiplySize(child ? 0.5F : 1.0F);
    }

    protected final void multiplySize(float size) {
        setSize(0.6F * size, 1.95F * size);
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        if (isChild()) experienceValue = (int) (experienceValue * 2.5F);
        return super.getExperiencePoints(player);
    }

    /** Whether it burns in daylight (husks and mods' zombies override this). */
    protected boolean shouldBurnInDay() {
        return true;
    }

    @Override
    public void onLivingUpdate() {
        if (mcc$hosted() && mcc$entity != null && shouldBurnInDay() && !mcc$entity.level().isClientSide() && mcc$entity.level().isDay() && !isChild()) {
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
    public boolean attackEntityAsMob(legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && mcc$entity != null && isBurning() && getHeldItemMainhand().isEmpty() && rand.nextFloat() < mcc$entity.level().getCurrentDifficultyAt(
                mcc$entity.blockPosition()).getEffectiveDifficulty() * 0.3F) {
            target.setFire(2 * (int) mcc$entity.level().getCurrentDifficultyAt(mcc$entity.blockPosition()).getEffectiveDifficulty());
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.ZOMBIE_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.ZOMBIE_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.ZOMBIE_DEATH);
    }

    protected SoundEvent getStepSound() {
        return legacy(SoundEvents.ZOMBIE_STEP);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute getCreatureAttribute() {
        return legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute.UNDEAD;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/zombie");
    }

    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        super.setEquipmentBasedOnDifficulty(difficulty);
        if (rand.nextFloat() < (mcc$entity != null && mcc$entity.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 0.05F : 0.01F)) {
            int i = rand.nextInt(3);
            var item = i == 0 ? legacyconnector.v1_12_2.net.minecraft.init.Items.IRON_SWORD : legacyconnector.v1_12_2.net.minecraft.init.Items.IRON_SHOVEL;
            setItemStackToSlot(legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.MAINHAND, new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item));
        }
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata) {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        if (mcc$hosted()) {
            float f = difficulty == null ? 0.0F : difficulty.getClampedAdditionalDifficulty();
            setCanPickUpLoot(rand.nextFloat() < 0.55F * f);
            if (rand.nextFloat() < 0.05F) setChild(true);
            setBreakDoorsAItask(rand.nextFloat() < f * 0.1F);
            if (difficulty != null) {
                setEquipmentBasedOnDifficulty(difficulty);
                setEnchantmentBasedOnDifficulty(difficulty);
            }
        }
        return livingdata;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$zombie() != null) return;
        if (isChild()) tag.setBoolean("IsBaby", true);
        tag.setBoolean("CanBreakDoors", isBreakDoorsTaskSet());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$zombie() != null) return;
        if (tag.getBoolean("IsBaby")) setChild(true);
        setBreakDoorsAItask(tag.getBoolean("CanBreakDoors"));
    }

    @Override
    public float getEyeHeight() {
        return mcc$zombie() != null ? super.getEyeHeight() : isChild() ? 0.93F : 1.74F;
    }

    @Override
    public double getYOffset() {
        return isChild() ? 0.0D : -0.45D;
    }
}
