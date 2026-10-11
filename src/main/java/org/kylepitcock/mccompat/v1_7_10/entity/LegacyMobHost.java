package org.kylepitcock.mccompat.v1_7_10.entity;

import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.EntityLiving;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.SoundBridge;

/**
 * Host of a legacy living entity. Being a vanilla {@link PathfinderMob} gives the legacy entity real navigation,
 * movement/look/jump control, attributes, equipment, combat and despawning; the legacy object's overrides run in
 * their place (tick -> onUpdate, aiStep -> onLivingUpdate, AI step -> updateAITasks / updateEntityActionState, ...).
 */
public class LegacyMobHost extends PathfinderMob implements LegacyLivingHost, IEntityWithComplexSpawn,
        net.minecraft.world.entity.monster.RangedAttackMob {
    static final EntityDataAccessor<CompoundTag> WATCHER = SynchedEntityData.defineId(LegacyMobHost.class, EntityDataSerializers.COMPOUND_TAG);

    private Entity legacy;
    private float width = -1.0F;
    private float height = -1.0F;
    private ServerLevel dropLevel;
    private DamageSource dropSource;
    private boolean dropRecentlyHit;

    public LegacyMobHost(EntityType<? extends LegacyMobHost> type, Level level, Entity adopted) {
        super(type, level);
        if (adopted != null) {
            legacy = adopted;
        } else {
            Class<?> cls = LegacyHosts.classFor(type);
            if (cls != null) legacy = LegacyHosts.construct(cls, this, level);
        }
        if (legacy != null) {
            EntityBridge.bind(legacy, this);
            if (!level.isClientSide()) entityData.set(WATCHER, legacy.mcc$dataWatcher().mcc$toTag());
        }
    }

    @Override
    public Entity mcc$legacy() {
        return legacy;
    }

    private EntityLivingBase living() {
        return legacy instanceof EntityLivingBase l ? l : null;
    }

    private EntityPlayer wrap(Player player) {
        return (EntityPlayer) EntityBridge.wrap(player);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WATCHER, new CompoundTag());
    }

    @Override
    public void mcc$syncWatcher(CompoundTag values) {
        entityData.set(WATCHER, values);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == WATCHER && legacy != null && level().isClientSide()) legacy.mcc$dataWatcher().mcc$fromTag(entityData.get(WATCHER));
    }

    @Override
    protected void registerGoals() {
        // Legacy AI tasks are scheduled by the legacy entity itself (EntityAITasks).
    }

    // ------------------------------------------------------------------ ticking

    private int failedTicks;

    @Override
    public void tick() {
        if (legacy == null) {
            if (!level().isClientSide()) discard();
            return;
        }
        try {
            legacy.onUpdate();
            failedTicks = 0;
        } catch (RuntimeException | LinkageError e) {
            if (LegacyHosts.tickFailed(this, legacy.getClass(), e, ++failedTicks)) discard();
        }
    }

    @Override
    protected net.minecraft.network.chat.Component getTypeName() {
        var name = LegacyHosts.displayName(getType());
        return name != null ? name : super.getTypeName();
    }

    @Override
    public void mcc$superTick() {
        super.tick();
    }

    @Override
    public void baseTick() {
        if (legacy == null) super.baseTick();
        else legacy.onEntityUpdate();
    }

    @Override
    public void mcc$superBaseTick() {
        super.baseTick();
    }

    @Override
    public void aiStep() {
        EntityLivingBase l = living();
        if (l == null) super.aiStep();
        else l.onLivingUpdate();
    }

    @Override
    public void mcc$superAiStep() {
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        if (legacy instanceof EntityLiving l) l.mcc$serverAiStep();
    }

    // ------------------------------------------------------------------ damage, death, drops

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (legacy == null) return super.hurt(source, amount);
        return legacy.attackEntityFrom(mccompat.v1_7_10.net.minecraft.util.DamageSource.mcc$wrap(source), amount);
    }

    @Override
    public boolean mcc$superHurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        EntityLivingBase l = living();
        if (l == null) super.die(source);
        else l.onDeath(mccompat.v1_7_10.net.minecraft.util.DamageSource.mcc$wrap(source));
    }

    @Override
    public void mcc$superDie(DamageSource source) {
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        EntityLivingBase l = living();
        if (l == null) {
            super.dropCustomDeathLoot(level, source, recentlyHit);
            return;
        }
        dropLevel = level;
        dropSource = source;
        dropRecentlyHit = recentlyHit;
        try {
            l.mcc$dropLoot(recentlyHit, 0);
        } finally {
            dropLevel = null;
            dropSource = null;
        }
    }

    @Override
    public void mcc$superDropEquipment() {
        if (dropLevel != null) super.dropCustomDeathLoot(dropLevel, dropSource, dropRecentlyHit);
    }

    @Override
    protected int getBaseExperienceReward() {
        return legacy instanceof EntityLiving l ? l.mcc$experience() : super.getBaseExperienceReward();
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 input) {
        EntityLivingBase l = living();
        if (l == null || inTravel) {
            super.travel(input);
            return;
        }
        l.moveEntityWithHeading((float) input.x, (float) input.z);
    }

    private boolean inTravel;

    public void mcc$jump() {
        jumpFromGround();
    }

    @Override
    public void mcc$superTravel(float strafe, float forward) {
        inTravel = true;
        try {
            super.travel(new net.minecraft.world.phys.Vec3(strafe, 0.0, forward));
        } finally {
            inTravel = false;
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (legacy instanceof mccompat.v1_7_10.net.minecraft.entity.IRangedAttackMob r) {
            r.attackEntityWithRangedAttack((EntityLivingBase) EntityBridge.wrap(target), power);
        }
    }

    @Override
    public void mcc$superSetTarget(LivingEntity target) {
        super.setTarget(target);
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy instanceof EntityLiving l ? SoundBridge.sound(l.mcc$livingSound()) : super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        EntityLivingBase l = living();
        return l != null ? SoundBridge.sound(l.mcc$hurtSound()) : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        EntityLivingBase l = living();
        return l != null ? SoundBridge.sound(l.mcc$deathSound()) : super.getDeathSound();
    }

    @Override
    protected float getSoundVolume() {
        EntityLivingBase l = living();
        return l != null ? l.mcc$soundVolume() : super.getSoundVolume();
    }

    @Override
    public float getVoicePitch() {
        EntityLivingBase l = living();
        return l != null ? l.mcc$soundPitch() : super.getVoicePitch();
    }

    @Override
    public int getAmbientSoundInterval() {
        return legacy instanceof EntityLiving l ? l.getTalkInterval() : super.getAmbientSoundInterval();
    }

    // ------------------------------------------------------------------ despawning, interaction, size

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
        if (!(legacy instanceof EntityLiving l)) return super.checkSpawnRules(level, reason);
        // During chunk generation `level` is the generation region, not the entity's level.
        boolean region = !(level instanceof ServerLevel);
        if (region) l.mcc$worldOverride = level;
        try {
            return l.getCanSpawnHere();
        } finally {
            if (region) l.mcc$worldOverride = null;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        return legacy instanceof EntityLiving l ? l.mcc$canDespawn() : super.removeWhenFarAway(distanceSq);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || legacy instanceof EntityLiving l && l.isNoDespawnRequired();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (legacy == null || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        return legacy.interactFirst(wrap(player)) ? InteractionResult.sidedSuccess(level().isClientSide()) : InteractionResult.PASS;
    }

    @Override
    public void playerTouch(Player player) {
        if (legacy != null) legacy.onCollideWithPlayer(wrap(player));
    }

    @Override
    public boolean isPickable() {
        return legacy == null ? super.isPickable() : legacy.canBeCollidedWith();
    }

    @Override
    public boolean isPushable() {
        return legacy == null ? super.isPushable() : legacy.canBePushed();
    }

    @Override
    public void mcc$setSize(float width, float height) {
        if (width == this.width && height == this.height) return;
        this.width = width;
        this.height = height;
        refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return width > 0 ? EntityDimensions.scalable(width, height) : super.getDefaultDimensions(pose);
    }

    // ------------------------------------------------------------------ saving and spawn data

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (legacy != null) {
            legacy.mcc$writeEntityToNBT(new NBTTagCompound(tag));
            legacy.mcc$saveExtended(new NBTTagCompound(tag));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (legacy != null) {
            legacy.mcc$readEntityFromNBT(new NBTTagCompound(tag));
            legacy.mcc$loadExtended(new NBTTagCompound(tag));
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        LegacySpawnData.write(this, legacy, buf);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        Entity read = LegacySpawnData.read(this, legacy, buf);
        if (legacy == null && read != null) {
            legacy = read;
            EntityBridge.bind(legacy, this);
        }
    }
}
