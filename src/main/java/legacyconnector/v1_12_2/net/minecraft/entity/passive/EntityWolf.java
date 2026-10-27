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

/** 1.12.2 wolf: views read the modern wolf (anger, collar, begging, wetness animations); mods' subclasses keep the 1.12 state. */
public class EntityWolf extends EntityTameable {
    public EntityWolf(World world) {
        super(world);
        setSize(0.6F, 0.85F);
    }

    protected EntityWolf(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.Wolf mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.Wolf m ? m : null;
    }

    private static final DataParameter<Float> DATA_HEALTH_ID = EntityDataManager.createKey(EntityWolf.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> BEGGING = EntityDataManager.createKey(EntityWolf.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> COLLAR_COLOR = EntityDataManager.createKey(EntityWolf.class, DataSerializers.VARINT);
    private float headRotationCourse;
    private float headRotationCourseOld;
    private boolean isWet;
    private boolean isShaking;
    private float timeWolfIsShaking;
    private float prevTimeWolfIsShaking;

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(DATA_HEALTH_ID, 8.0F);
        dataManager.register(BEGGING, false);
        dataManager.register(COLLAR_COLOR, 14);
    }

    @Override
    protected void initEntityAI() {
        aiSit = new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISit(this);
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(2, aiSit);
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILeapAtTarget(this, 0.4F));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAttackMelee(this, 1.0D, true));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIFollowOwner(this, 1.0D, 10.0F, 2.0F));
        tasks.addTask(7, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMate(this, 1.0D));
        tasks.addTask(8, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(10, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(10, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
        targetTasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIOwnerHurtByTarget(this));
        targetTasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIOwnerHurtTarget(this));
        targetTasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget(this, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(8.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2.0D);
    }

    public boolean isAngry() {
        var m = mcc$self();
        return m != null ? m.isAngry() : getAttackTarget() != null && !isTamed();
    }

    public void setAngry(boolean angry) {
        var m = mcc$self();
        if (m != null) {
            if (angry) m.startPersistentAngerTimer();
            else m.stopBeingAngry();
        }
    }

    public legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor getCollarColor() {
        var m = mcc$self();
        if (m != null) return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byMetadata(m.getCollarColor().getId());
        Integer c = dataManager.get(COLLAR_COLOR);
        return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byDyeDamage(c == null ? 14 : c);
    }

    /** Hosted wolves keep the colour; the modern wolf's collar is only dyed by players. */
    public void setCollarColor(legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor color) {
        if (color != null) dataManager.set(COLLAR_COLOR, color.getDyeDamage());
    }

    public boolean isBegging() {
        var m = mcc$self();
        return m != null ? m.isInterested() : Boolean.TRUE.equals(dataManager.get(BEGGING));
    }

    public void setBegging(boolean begging) {
        dataManager.set(BEGGING, begging);
    }

    public boolean isWolfWet() {
        var m = mcc$self();
        return m != null ? m.isWet() : isWet;
    }

    /** Head tilt while begging (client animation). */
    public float getInterestedAngle(float partialTicks) {
        var m = mcc$self();
        if (m != null) return m.getHeadRollAngle(partialTicks);
        return (headRotationCourseOld + (headRotationCourse - headRotationCourseOld) * partialTicks) * 0.15F * (float) Math.PI;
    }

    /** Body roll while shaking off water (client animation). */
    public float getShakeAngle(float partialTicks, float offset) {
        var m = mcc$self();
        if (m != null) return m.getBodyRollAngle(partialTicks, offset);
        float f = (prevTimeWolfIsShaking + (timeWolfIsShaking - prevTimeWolfIsShaking) * partialTicks + offset) / 1.8F;
        f = Math.max(0.0F, Math.min(1.0F, f));
        return (float) (Math.sin(f * Math.PI) * Math.sin(f * Math.PI * 11.0D) * 0.15F * Math.PI);
    }

    public float getTailRotation() {
        var m = mcc$self();
        if (m != null) return m.getTailAngle();
        if (isAngry()) return 1.5393804F;
        return isTamed() ? (0.55F - (getMaxHealth() - getHealth()) * 0.02F) * (float) Math.PI : (float) Math.PI / 5F;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (mcc$self() == null) {
            headRotationCourseOld = headRotationCourse;
            headRotationCourse += ((isBegging() ? 1.0F : 0.0F) - headRotationCourse) * 0.4F;
            isWet = isWet();
            prevTimeWolfIsShaking = timeWolfIsShaking;
            if (isShaking) {
                timeWolfIsShaking += 0.05F;
                if (timeWolfIsShaking > 2.0F) {
                    isShaking = false;
                    timeWolfIsShaking = 0.0F;
                    prevTimeWolfIsShaking = 0.0F;
                }
            } else if (isWet && !isWet() && mcc$get$onGround(this)) {
                isShaking = true;
                timeWolfIsShaking = 0.0F;
                prevTimeWolfIsShaking = 0.0F;
            }
        }
    }

    @Override
    public boolean attackEntityAsMob(legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        if (target == null) return false;
        var damage = getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage == null ? 2.0F : (float) damage.getAttributeValue());
        if (hit) applyEnchantments(this, target);
        return hit;
    }

    @Override
    public void setTamed(boolean tamed) {
        super.setTamed(tamed);
        var health = getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (mcc$hosted() && health != null) health.setBaseValue(tamed ? 20.0D : 8.0D);
    }

    @Override
    public boolean shouldAttackEntity(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase owner) {
        if (target instanceof legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityCreeper) return false;
        if (target instanceof EntityWolf w && w.isTamed() && w.getOwner() == owner) return false;
        if (target instanceof EntityPlayer p && owner instanceof EntityPlayer o && !o.canAttackPlayer(p)) return false;
        return !(target instanceof EntityTameable t && t.isTamed());
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() == null) tag.setByte("CollarColor", (byte) getCollarColor().getDyeDamage());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() == null && tag.hasKey("CollarColor", 99)) setCollarColor(legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byDyeDamage(tag.getByte("CollarColor")));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (isAngry()) return legacy(SoundEvents.WOLF_GROWL);
        if (rand.nextInt(3) == 0) return legacy(isTamed() && getHealth() < 10.0F ? SoundEvents.WOLF_WHINE : SoundEvents.WOLF_PANT);
        return legacy(SoundEvents.WOLF_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.WOLF_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.WOLF_DEATH);
    }

    @Override
    public float getEyeHeight() {
        return mcc$get$height(this) * 0.8F;
    }

    @Override
    public boolean isBreedingItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        if (!mcc$hosted()) return super.isBreedingItem(stack);
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack).is(net.minecraft.tags.ItemTags.WOLF_FOOD);
    }

    @Override
    public EntityWolf createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityWolf(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/wolf");
    }
}
