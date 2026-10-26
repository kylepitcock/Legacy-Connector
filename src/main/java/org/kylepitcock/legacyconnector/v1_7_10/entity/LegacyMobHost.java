package org.kylepitcock.legacyconnector.v1_7_10.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
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
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.SoundBridge;

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
    /** The boss bar of a legacy boss (IBossDisplayData): 1.7.10 showed it to every player that had the boss loaded, not only to those next to it. */
    private net.minecraft.server.level.ServerBossEvent bossEvent;

    private void updateBossBar() {
        if (!(level() instanceof ServerLevel sl) || !(legacy instanceof legacyconnector.v1_7_10.net.minecraft.entity.boss.IBossDisplayData boss)) return;
        if (bossEvent == null) {
            bossEvent = new net.minecraft.server.level.ServerBossEvent(getDisplayName(), net.minecraft.world.BossEvent.BossBarColor.PURPLE,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
        }
        float max = boss.getMaxHealth();
        bossEvent.setProgress(max <= 0 ? 0 : Math.max(0, Math.min(1, boss.getHealth() / max)));
        bossEvent.setName(getDisplayName());
        if (tickCount % 20 != 0) return;
        for (var player : sl.players()) {
            boolean near = player.distanceToSqr(this) < 128.0 * 128.0;
            if (near) bossEvent.addPlayer(player);
            else bossEvent.removePlayer(player);
        }
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (bossEvent != null) bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void tick() {
        if (legacy == null) {
            if (!level().isClientSide()) discard();
            return;
        }
        try {
            legacy.onUpdate();
            failedTicks = 0;
            updateBossBar();
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
        return legacy.attackEntityFrom(legacyconnector.v1_7_10.net.minecraft.util.DamageSource.mcc$wrap(source), amount);
    }

    @Override
    public boolean mcc$superHurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        EntityLivingBase l = living();
        if (l == null) super.actuallyHurt(source, amount);
        else l.mcc$damageEntity(legacyconnector.v1_7_10.net.minecraft.util.DamageSource.mcc$wrap(source), amount);
    }

    @Override
    public void mcc$superActuallyHurt(DamageSource source, float amount) {
        // legacy code calls damageEntity directly (Thaumcraft's taintacles wither away): NeoForge's actuallyHurt reads
        // the damage container that only hurt() pushes
        if (!damageContainers.isEmpty()) {
            super.actuallyHurt(source, amount);
            return;
        }
        damageContainers.push(new net.neoforged.neoforge.common.damagesource.DamageContainer(source, amount));
        try {
            super.actuallyHurt(source, amount);
        } finally {
            damageContainers.pop();
        }
    }

    @Override
    public void die(DamageSource source) {
        EntityLivingBase l = living();
        if (l == null) super.die(source);
        else l.onDeath(legacyconnector.v1_7_10.net.minecraft.util.DamageSource.mcc$wrap(source));
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
        if (legacy instanceof legacyconnector.v1_7_10.net.minecraft.entity.IRangedAttackMob r) {
            r.attackEntityWithRangedAttack((EntityLivingBase) EntityBridge.wrap(target), power);
        }
    }

    @Override
    public void mcc$superSetTarget(LivingEntity target) {
        super.setTarget(target);
    }

    // ------------------------------------------------------------------ falling, the void, lightning, riding

    private final LegacyHookState hooks = new LegacyHookState();

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (legacy == null || hooks.inFall) return super.causeFallDamage(distance, multiplier, source);
        return hooks.fall(legacy, distance, multiplier, source);
    }

    @Override
    public void mcc$superFall(float distance) {
        hooks.superFall(distance, (d, m, s) -> super.causeFallDamage(d, m, s), damageSources().fall());
    }

    @Override
    protected void onBelowWorld() {
        if (legacy == null) super.onBelowWorld();
        else legacy.mcc$kill();
    }

    @Override
    public void mcc$superKill() {
        super.onBelowWorld();
    }

    @Override
    public void thunderHit(ServerLevel level, net.minecraft.world.entity.LightningBolt bolt) {
        if (legacy == null) super.thunderHit(level, bolt);
        else legacy.onStruckByLightning(LegacyHookState.wrapBolt(bolt));
    }

    @Override
    public void mcc$superThunderHit(ServerLevel level, net.minecraft.world.entity.LightningBolt bolt) {
        super.thunderHit(level, bolt);
    }

    @Override
    protected void positionRider(net.minecraft.world.entity.Entity passenger, net.minecraft.world.entity.Entity.MoveFunction callback) {
        if (legacy != null && LegacyHookState.customRider(legacy)) legacy.updateRiderPosition();
        else super.positionRider(passenger, callback);
    }

    @Override
    public void mcc$superPositionRider(net.minecraft.world.entity.Entity passenger) {
        super.positionRider(passenger, net.minecraft.world.entity.Entity::setPos);
    }

    @Override
    public void rideTick() {
        if (legacy != null && LegacyHosts.overrides(legacy.getClass(), "updateRidden")) legacy.updateRidden();
        else super.rideTick();
    }

    @Override
    public void mcc$superRideTick() {
        super.rideTick();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        if (legacy != null && LegacyHookState.customLerp(legacy)) legacy.setPositionAndRotation2(x, y, z, yaw, pitch, steps);
        else super.lerpTo(x, y, z, yaw, pitch, steps);
    }

    @Override
    public void mcc$superLerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        super.lerpTo(x, y, z, yaw, pitch, steps);
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (legacy != null && LegacyHookState.customStepSound(legacy)) {
            legacy.mcc$playStepSound(pos.getX(), pos.getY(), pos.getZ(), org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.legacyBlock(state));
        } else {
            super.playStepSound(pos, state);
        }
    }

    @Override
    public boolean isPushedByFluid() {
        if (legacy != null && LegacyHosts.overrides(legacy.getClass(), "isPushedByWater")) return legacy.isPushedByWater();
        return super.isPushedByFluid();
    }

    // ------------------------------------------------------------------ collisions, effects, kills

    private static final Class<?>[] DAMAGE_PARAMS = {legacyconnector.v1_7_10.net.minecraft.util.DamageSource.class, float.class};

    @Override
    protected void doPush(net.minecraft.world.entity.Entity other) {
        EntityLivingBase l = living();
        if (l != null && LegacyHosts.overrides(legacy.getClass(), "collideWithEntity", Entity.class)) l.mcc$collideWithEntity(EntityBridge.wrap(other));
        else super.doPush(other);
    }

    @Override
    public void mcc$superDoPush(net.minecraft.world.entity.Entity other) {
        super.doPush(other);
    }

    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float amount) {
        EntityLivingBase l = living();
        if (l != null && LegacyHosts.overrides(legacy.getClass(), "applyPotionDamageCalculations", DAMAGE_PARAMS)) {
            return l.mcc$applyPotionDamageCalculations(legacyconnector.v1_7_10.net.minecraft.util.DamageSource.mcc$wrap(source), amount);
        }
        return super.getDamageAfterMagicAbsorb(source, amount);
    }

    @Override
    public float mcc$superMagicAbsorb(DamageSource source, float amount) {
        return super.getDamageAfterMagicAbsorb(source, amount);
    }

    @Override
    protected int decreaseAirSupply(int air) {
        EntityLivingBase l = living();
        if (l != null && LegacyHosts.overrides(legacy.getClass(), "decreaseAirSupply", int.class)) return l.mcc$decreaseAirSupply(air);
        return super.decreaseAirSupply(air);
    }

    @Override
    public int mcc$superDecreaseAirSupply(int air) {
        return super.decreaseAirSupply(air);
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        EntityLivingBase l = living();
        if (l != null && type == net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value() && l.canBreatheUnderwater()) return false;
        return super.canDrownInFluidType(type);
    }

    @Override
    protected void tickDeath() {
        EntityLivingBase l = living();
        if (l != null && LegacyHosts.overrides(legacy.getClass(), "onDeathUpdate")) l.mcc$onDeathUpdate();
        else super.tickDeath();
    }

    @Override
    public void mcc$superTickDeath() {
        super.tickDeath();
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        EntityLivingBase l = living();
        if (l != null && LegacyHosts.overrides(legacy.getClass(), "isPotionApplicable", legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect.class)) {
            var wrapped = legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect.mcc$wrap(effect);
            if (wrapped != null) return l.isPotionApplicable(wrapped);
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean result = super.killedEntity(level, victim);
        if (legacy != null && EntityBridge.wrap(victim) instanceof EntityLivingBase v) legacy.onKillEntity(v);
        return result;
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
            try {
                legacy.mcc$writeEntityToNBT(new NBTTagCompound(tag));
                legacy.mcc$saveExtended(new NBTTagCompound(tag));
            } catch (RuntimeException | LinkageError e) {
                LegacyHosts.nbtFailure(this, "writing", e);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (legacy != null) {
            try {
                legacy.mcc$readEntityFromNBT(new NBTTagCompound(tag));
                legacy.mcc$loadExtended(new NBTTagCompound(tag));
            } catch (RuntimeException | LinkageError e) {
                LegacyHosts.nbtFailure(this, "reading", e);
            }
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
