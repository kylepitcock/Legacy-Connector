package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
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
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/**
 * Host of a 1.12.2 living entity. Being a vanilla {@link PathfinderMob} gives the legacy entity real navigation, movement, look and
 * jump control, attributes, equipment, combat and despawning; the legacy object's overrides run in their place (tick -> onUpdate,
 * aiStep -> onLivingUpdate, the server AI step -> the legacy task lists and updateAITasks, ...).
 */
public class MobHost1122 extends PathfinderMob implements LivingHost1122, IEntityWithComplexSpawn, net.minecraft.world.entity.monster.RangedAttackMob {
    static final EntityDataAccessor<CompoundTag> DATA = SynchedEntityData.defineId(MobHost1122.class, EntityDataSerializers.COMPOUND_TAG);

    private Entity legacy;
    private float width = -1.0F;
    private float height = -1.0F;
    private int failedTicks;
    private final HookState1122 hooks = new HookState1122();
    private ServerLevel dropLevel;
    private DamageSource dropSource;
    private boolean dropRecentlyHit;

    public MobHost1122(EntityType<? extends MobHost1122> type, Level level, Entity adopted) {
        super(type, level);
        if (adopted != null) {
            legacy = adopted;
        } else {
            Class<?> cls = Hosts1122.classFor(type);
            if (cls != null) legacy = Hosts1122.construct(cls, this, level);
        }
        if (legacy != null) EntityBridge1122.bind(legacy, this);
    }

    @Override
    public Entity mcc$legacy() {
        return legacy;
    }

    private EntityLivingBase living() {
        return legacy instanceof EntityLivingBase l ? l : null;
    }

    private static legacyconnector.v1_12_2.net.minecraft.util.DamageSource legacySource(DamageSource source) {
        return legacyconnector.v1_12_2.net.minecraft.util.DamageSource.mcc$of(source);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == DATA && legacy != null && level().isClientSide()) DataSync1122.apply(legacy, entityData.get(DATA));
    }

    @Override
    protected void registerGoals() {
        // the legacy entity schedules its own AI tasks (EntityAITasks)
    }

    // ------------------------------------------------------------------ ticking

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
            if (Hosts1122.tickFailed(this, legacy.getClass(), e, ++failedTicks)) discard();
        }
        if (!level().isClientSide()) {
            CompoundTag changed = DataSync1122.changed(legacy);
            if (changed != null) entityData.set(DATA, changed);
        }
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

    @Override
    protected net.minecraft.network.chat.Component getTypeName() {
        var name = Hosts1122.displayName(getType());
        return name != null ? name : super.getTypeName();
    }

    // ------------------------------------------------------------------ damage, death, drops

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (legacy == null) return super.hurt(source, amount);
        return legacy.attackEntityFrom(legacySource(source), amount);
    }

    @Override
    public boolean mcc$superHurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        EntityLivingBase l = living();
        if (l == null || !Hosts1122.overrides(legacy.getClass(), "damageEntity", legacyconnector.v1_12_2.net.minecraft.util.DamageSource.class, float.class)) {
            super.actuallyHurt(source, amount);
        } else {
            l.mcc$damageEntity(legacySource(source), amount);
        }
    }

    @Override
    public void mcc$superActuallyHurt(DamageSource source, float amount) {
        // legacy code calls damageEntity directly: NeoForge's actuallyHurt reads the damage container that only hurt() pushes
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
        else l.onDeath(legacySource(source));
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
            int looting = 0;
            if (source.getEntity() instanceof LivingEntity killer) {
                var enchantments = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                        enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), killer);
            }
            l.mcc$dropLoot(recentlyHit, looting, legacySource(source));
        } catch (RuntimeException | LinkageError e) {
            Hosts1122.failed(legacy.getClass(), "dropping its loot", e);
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
    protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> getDefaultLootTable() {
        if (legacy instanceof EntityLiving l) {
            var table = l.mcc$lootTable();
            if (table != null) {
                return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(org.kylepitcock.legacyconnector.core.Ids.modernModId(table.getNamespace()),
                                table.getPath()));
            }
        }
        return super.getDefaultLootTable();
    }

    @Override
    protected int getBaseExperienceReward() {
        EntityLivingBase l = living();
        return l != null ? l.mcc$experience() : super.getBaseExperienceReward();
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        EntityLivingBase l = living();
        if (l != null && Hosts1122.overrides(legacy.getClass(), "attackEntityAsMob", Entity.class)) {
            Entity t = EntityBridge1122.wrap(target);
            return t != null && l.attackEntityAsMob(t);
        }
        return super.doHurtTarget(target);
    }

    @Override
    public boolean mcc$superDoHurtTarget(net.minecraft.world.entity.Entity target) {
        return super.doHurtTarget(target);
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 input) {
        EntityLivingBase l = living();
        if (l == null || inTravel || !Hosts1122.overrides(legacy.getClass(), "travel", float.class, float.class, float.class)) {
            super.travel(input);
            return;
        }
        l.travel((float) input.x, (float) input.y, (float) input.z);
    }

    private boolean inTravel;

    @Override
    public void mcc$superTravel(float strafe, float vertical, float forward) {
        inTravel = true;
        try {
            super.travel(new net.minecraft.world.phys.Vec3(strafe, vertical, forward));
        } finally {
            inTravel = false;
        }
    }

    @Override
    public void jumpFromGround() {
        EntityLivingBase l = living();
        if (l != null && !inJump && Hosts1122.overrides(legacy.getClass(), "jump")) {
            inJump = true;
            try {
                l.mcc$jump();
            } finally {
                inJump = false;
            }
        } else {
            super.jumpFromGround();
        }
    }

    private boolean inJump;

    @Override
    public void mcc$superJump() {
        super.jumpFromGround();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (legacy instanceof legacyconnector.v1_12_2.net.minecraft.entity.IRangedAttackMob r
                && EntityBridge1122.wrap(target) instanceof EntityLivingBase t) {
            r.attackEntityWithRangedAttack(t, power);
        }
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (legacy instanceof EntityLiving l && !inSetTarget && Hosts1122.overrides(legacy.getClass(), "setAttackTarget", EntityLivingBase.class)) {
            inSetTarget = true;
            try {
                l.setAttackTarget(target == null ? null : (EntityLivingBase) EntityBridge1122.wrap(target));
            } finally {
                inSetTarget = false;
            }
        } else {
            super.setTarget(target);
        }
    }

    private boolean inSetTarget;

    @Override
    public void mcc$superSetTarget(LivingEntity target) {
        super.setTarget(target);
    }

    // ------------------------------------------------------------------ falling, the void, lightning, riding

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (legacy == null || hooks.inFall) return super.causeFallDamage(distance, multiplier, source);
        return hooks.fall(legacy, distance, multiplier, source);
    }

    @Override
    public void mcc$superFall(float distance, float multiplier) {
        hooks.superFall(distance, multiplier, (d, m, s) -> super.causeFallDamage(d, m, s), damageSources().fall());
    }

    @Override
    protected void onBelowWorld() {
        if (legacy == null) super.onBelowWorld();
        else legacy.mcc$outOfWorld();
    }

    @Override
    public void mcc$superOutOfWorld() {
        super.onBelowWorld();
    }

    @Override
    public void thunderHit(ServerLevel level, net.minecraft.world.entity.LightningBolt bolt) {
        if (legacy == null) super.thunderHit(level, bolt);
        else legacy.onStruckByLightning(HookState1122.wrapBolt(bolt));
    }

    @Override
    public void mcc$superThunderHit(ServerLevel level, net.minecraft.world.entity.LightningBolt bolt) {
        super.thunderHit(level, bolt);
    }

    @Override
    protected void positionRider(net.minecraft.world.entity.Entity passenger, net.minecraft.world.entity.Entity.MoveFunction callback) {
        if (legacy != null && HookState1122.customRider(legacy)) legacy.updatePassenger(EntityBridge1122.wrap(passenger));
        else super.positionRider(passenger, callback);
    }

    @Override
    public void mcc$superPositionRider(net.minecraft.world.entity.Entity passenger) {
        super.positionRider(passenger, net.minecraft.world.entity.Entity::setPos);
    }

    @Override
    public void rideTick() {
        if (legacy != null && Hosts1122.overrides(legacy.getClass(), "updateRidden")) legacy.updateRidden();
        else super.rideTick();
    }

    @Override
    public void mcc$superRideTick() {
        super.rideTick();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        if (legacy != null && HookState1122.customLerp(legacy)) legacy.setPositionAndRotationDirect(x, y, z, yaw, pitch, steps, false);
        else super.lerpTo(x, y, z, yaw, pitch, steps);
    }

    @Override
    public void mcc$superLerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        super.lerpTo(x, y, z, yaw, pitch, steps);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (legacy != null && Hosts1122.overrides(legacy.getClass(), "handleStatusUpdate", byte.class)) legacy.handleStatusUpdate(id);
        else super.handleEntityEvent(id);
    }

    @Override
    public void mcc$superHandleEntityEvent(byte id) {
        super.handleEntityEvent(id);
    }

    @Override
    public boolean isPushedByFluid() {
        if (legacy != null && Hosts1122.overrides(legacy.getClass(), "isPushedByWater")) return legacy.isPushedByWater();
        return super.isPushedByFluid();
    }

    @Override
    public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (legacy != null) HookState1122.startTracking(legacy, player);
    }

    @Override
    public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (legacy != null) HookState1122.stopTracking(legacy, player);
    }

    // ------------------------------------------------------------------ collisions, effects, kills

    private static final Class<?>[] DAMAGE_PARAMS = {legacyconnector.v1_12_2.net.minecraft.util.DamageSource.class, float.class};

    @Override
    protected void doPush(net.minecraft.world.entity.Entity other) {
        EntityLivingBase l = living();
        if (l != null && Hosts1122.overrides(legacy.getClass(), "collideWithEntity", Entity.class)) l.mcc$collideWithEntity(EntityBridge1122.wrap(other));
        else super.doPush(other);
    }

    @Override
    public void mcc$superDoPush(net.minecraft.world.entity.Entity other) {
        super.doPush(other);
    }

    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float amount) {
        EntityLivingBase l = living();
        if (l != null && Hosts1122.overrides(legacy.getClass(), "applyPotionDamageCalculations", DAMAGE_PARAMS)) {
            return l.mcc$applyPotionDamageCalculations(legacySource(source), amount);
        }
        return super.getDamageAfterMagicAbsorb(source, amount);
    }

    @Override
    public float mcc$superMagicAbsorb(DamageSource source, float amount) {
        return super.getDamageAfterMagicAbsorb(source, amount);
    }

    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        EntityLivingBase l = living();
        if (l != null && Hosts1122.overrides(legacy.getClass(), "applyArmorCalculations", DAMAGE_PARAMS)) {
            return l.mcc$applyArmorCalculations(legacySource(source), amount);
        }
        return super.getDamageAfterArmorAbsorb(source, amount);
    }

    @Override
    public float mcc$superArmorAbsorb(DamageSource source, float amount) {
        return super.getDamageAfterArmorAbsorb(source, amount);
    }

    @Override
    protected int decreaseAirSupply(int air) {
        EntityLivingBase l = living();
        if (l != null && Hosts1122.overrides(legacy.getClass(), "decreaseAirSupply", int.class)) return l.mcc$decreaseAirSupply(air);
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
        if (l != null && Hosts1122.overrides(legacy.getClass(), "onDeathUpdate")) l.mcc$onDeathUpdate();
        else super.tickDeath();
    }

    @Override
    public void mcc$superTickDeath() {
        super.tickDeath();
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        EntityLivingBase l = living();
        if (l != null && Hosts1122.overrides(legacy.getClass(), "isPotionApplicable", legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect.class)) {
            var wrapped = legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect.mcc$fromModern(effect);
            if (wrapped != null) return l.isPotionApplicable(wrapped);
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean isInvertedHealAndHarm() {
        EntityLivingBase l = living();
        if (l != null && Hosts1122.overrides(legacy.getClass(), "isEntityUndead")) return l.isEntityUndead();
        if (l != null && Hosts1122.overrides(legacy.getClass(), "getCreatureAttribute")) {
            return l.getCreatureAttribute() == legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute.UNDEAD;
        }
        return super.isInvertedHealAndHarm();
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean result = super.killedEntity(level, victim);
        if (legacy != null && EntityBridge1122.wrap(victim) instanceof EntityLivingBase v) legacy.onKillEntity(v);
        return result;
    }

    @Override
    public boolean fireImmune() {
        return legacy != null ? legacy.mcc$fireImmune() || super.fireImmune() : super.fireImmune();
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy instanceof EntityLiving l ? Sounds1122.modern(l.mcc$ambientSound()) : super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        EntityLivingBase l = living();
        return l != null ? Sounds1122.modern(l.mcc$hurtSound(legacySource(source))) : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        EntityLivingBase l = living();
        return l != null ? Sounds1122.modern(l.mcc$deathSound()) : super.getDeathSound();
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

    // ------------------------------------------------------------------ spawning, despawning, interaction, size

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
        if (!(legacy instanceof EntityLiving l)) return super.checkSpawnRules(level, reason);
        // during chunk generation `level` is the generation region, not the entity's level
        boolean region = !(level instanceof ServerLevel);
        if (region) l.mcc$worldOverride = level;
        try {
            return l.getCanSpawnHere();
        } catch (RuntimeException | LinkageError e) {
            return false;
        } finally {
            if (region) l.mcc$worldOverride = null;
        }
    }

    @Override
    public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level) {
        if (!(legacy instanceof EntityLiving l) || !(level instanceof ServerLevel)) return super.checkSpawnObstruction(level);
        try {
            return l.isNotColliding();
        } catch (RuntimeException | LinkageError e) {
            return super.checkSpawnObstruction(level);
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
        if (!(legacy instanceof EntityLiving l) || !(EntityBridge1122.wrap(player) instanceof EntityPlayer p)) return InteractionResult.PASS;
        try {
            return l.mcc$processInteract(p, HookState1122.hand(hand)) ? InteractionResult.sidedSuccess(level().isClientSide()) : InteractionResult.PASS;
        } finally {
            LiveStacks1122.flushAll();
        }
    }

    @Override
    public InteractionResult mcc$superMobInteract(Player player, InteractionHand hand) {
        return super.mobInteract(player, hand);
    }

    @Override
    public InteractionResult interactAt(Player player, net.minecraft.world.phys.Vec3 vec, InteractionHand hand) {
        if (legacy == null) return InteractionResult.PASS;
        return HookState1122.interactAt(legacy, player, vec, hand);
    }

    @Override
    public void playerTouch(Player player) {
        if (legacy != null && EntityBridge1122.wrap(player) instanceof EntityPlayer p) legacy.onCollideWithPlayer(p);
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
        if (legacy == null) return;
        try {
            legacy.mcc$writeEntityToNBT(new NBTTagCompound(tag));
            legacy.mcc$writeCaps(new NBTTagCompound(tag));
        } catch (RuntimeException | LinkageError e) {
            Hosts1122.nbtFailure(this, "writing", e);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (legacy == null) return;
        try {
            legacy.mcc$readEntityFromNBT(new NBTTagCompound(tag));
            legacy.mcc$readCaps(new NBTTagCompound(tag));
        } catch (RuntimeException | LinkageError e) {
            Hosts1122.nbtFailure(this, "reading", e);
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        SpawnData1122.write(this, legacy, buf);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        Entity read = SpawnData1122.read(this, legacy, buf);
        if (legacy == null && read != null) {
            legacy = read;
            EntityBridge1122.bind(legacy, this);
        }
    }
}
