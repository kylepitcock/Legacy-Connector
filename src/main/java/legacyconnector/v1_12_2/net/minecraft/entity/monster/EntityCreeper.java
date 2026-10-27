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

/** 1.12.2 creeper. A view swells, ignites and explodes as the modern creeper; a mod's subclass runs the 1.12 fuse
 * (state, ignition, charged explosions, lingering potion clouds) itself. */
public class EntityCreeper extends EntityMob {
    public EntityCreeper(World world) {
        super(world);
        setSize(0.6F, 1.7F);
    }

    protected EntityCreeper(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Creeper mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Creeper m ? m : null;
    }

    private static final DataParameter<Integer> STATE = EntityDataManager.createKey(EntityCreeper.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> POWERED = EntityDataManager.createKey(EntityCreeper.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IGNITED = EntityDataManager.createKey(EntityCreeper.class, DataSerializers.BOOLEAN);
    private int lastActiveTime;
    private int timeSinceIgnited;
    private int fuseTime = 30;
    private int explosionRadius = 3;
    private int droppedSkulls;
    @Deprecated
    public EntityPlayer attackingPlayer;

    public static EntityPlayer mcc$get$attackingPlayer(EntityCreeper e) {
        var l = e.mcc$entity instanceof net.minecraft.world.entity.LivingEntity m ? m.getLastHurtByMob() : null;
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(l) instanceof EntityPlayer p ? p : null;
    }

    public static void mcc$set$attackingPlayer(EntityCreeper e, EntityPlayer v) {
        if (v != null) e.setRevengeTarget(v);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, -1);
        dataManager.register(POWERED, false);
        dataManager.register(IGNITED, false);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAICreeperSwell(this));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAvoidEntity(this, legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityOcelot.class, 6.0F, 1.0D, 1.2D));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAttackMelee(this, 1.0D, false));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 0.8D));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
        targetTasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        targetTasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIHurtByTarget(this, false));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    @Override
    public int getMaxFallHeight() {
        return getAttackTarget() == null ? 3 : 3 + (int) (getHealth() - 1.0F);
    }

    /** -1 idle, 1 swelling. */
    public int getCreeperState() {
        var m = mcc$self();
        if (m != null) return m.getSwellDir();
        Integer s = dataManager.get(STATE);
        return s == null ? -1 : s;
    }

    public void setCreeperState(int state) {
        var m = mcc$self();
        if (m != null) m.setSwellDir(state);
        else dataManager.set(STATE, state);
    }

    public boolean getPowered() {
        var m = mcc$self();
        return m != null ? m.isPowered() : Boolean.TRUE.equals(dataManager.get(POWERED));
    }

    public boolean hasIgnited() {
        var m = mcc$self();
        return m != null ? m.isIgnited() : Boolean.TRUE.equals(dataManager.get(IGNITED));
    }

    public void ignite() {
        var m = mcc$self();
        if (m != null) m.ignite();
        else dataManager.set(IGNITED, true);
    }

    /** How far the swelling has got, for the flash and the swollen model (0..1). */
    public float getCreeperFlashIntensity(float partialTicks) {
        var m = mcc$self();
        if (m != null) return m.getSwelling(partialTicks);
        return (lastActiveTime + (timeSinceIgnited - lastActiveTime) * partialTicks) / (fuseTime - 2);
    }

    @Override
    public void onUpdate() {
        if (mcc$hosted() && isEntityAlive()) {
            lastActiveTime = timeSinceIgnited;
            if (hasIgnited()) setCreeperState(1);
            int i = getCreeperState();
            if (i > 0 && timeSinceIgnited == 0) playSound(legacy(SoundEvents.CREEPER_PRIMED), 1.0F, 0.5F);
            timeSinceIgnited += i;
            if (timeSinceIgnited < 0) timeSinceIgnited = 0;
            if (timeSinceIgnited >= fuseTime) {
                timeSinceIgnited = fuseTime;
                explode();
            }
        }
        super.onUpdate();
    }

    private void explode() {
        var m = mcc$entity;
        if (m == null || m.level().isClientSide()) return;
        float f = getPowered() ? 2.0F : 1.0F;
        m.level().explode(m, m.getX(), m.getY(), m.getZ(), explosionRadius * f, net.minecraft.world.level.Level.ExplosionInteraction.MOB);
        setDead();
        spawnLingeringCloud();
    }

    private void spawnLingeringCloud() {
        var m = mcc$entity;
        if (!(m instanceof net.minecraft.world.entity.LivingEntity l) || l.getActiveEffects().isEmpty()) return;
        var cloud = new net.minecraft.world.entity.AreaEffectCloud(m.level(), m.getX(), m.getY(), m.getZ());
        cloud.setRadius(2.5F);
        cloud.setRadiusOnUse(-0.5F);
        cloud.setWaitTime(10);
        cloud.setDuration(cloud.getDuration() / 2);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        for (var effect : l.getActiveEffects()) cloud.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
        m.level().addFreshEntity(cloud);
    }

    @Override
    public void onStruckByLightning(legacyconnector.v1_12_2.net.minecraft.entity.effect.EntityLightningBolt bolt) {
        if (mcc$self() != null) {
            super.onStruckByLightning(bolt);
            return;
        }
        super.onStruckByLightning(bolt);
        dataManager.set(POWERED, true);
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        super.fall(distance, damageMultiplier);
        timeSinceIgnited = (int) (timeSinceIgnited + distance * 1.5F);
        if (timeSinceIgnited > fuseTime - 5) timeSinceIgnited = fuseTime - 5;
    }

    /** Whether a charged creeper's explosion may still drop a mob head (one per charged creeper). */
    public boolean ableToCauseSkullDrop() {
        return getPowered() && droppedSkulls < 1;
    }

    public void incrementDroppedSkulls() {
        ++droppedSkulls;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() != null) return;
        if (getPowered()) tag.setBoolean("powered", true);
        tag.setShort("Fuse", (short) fuseTime);
        tag.setByte("ExplosionRadius", (byte) explosionRadius);
        tag.setBoolean("ignited", hasIgnited());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() != null) return;
        dataManager.set(POWERED, tag.getBoolean("powered"));
        if (tag.hasKey("Fuse", 99)) fuseTime = tag.getShort("Fuse");
        if (tag.hasKey("ExplosionRadius", 99)) explosionRadius = tag.getByte("ExplosionRadius");
        if (tag.getBoolean("ignited")) ignite();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.CREEPER_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.CREEPER_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/creeper");
    }
}
