package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/** Host of a non-living 1.12.2 entity (projectiles, orbs, special items, vehicles, ...). */
public class EntityHost1122 extends net.minecraft.world.entity.Entity implements Host1122, IEntityWithComplexSpawn {
    static final EntityDataAccessor<CompoundTag> DATA = SynchedEntityData.defineId(EntityHost1122.class, EntityDataSerializers.COMPOUND_TAG);

    private Entity legacy;
    private float width = -1.0F;
    private float height = -1.0F;
    private int failedTicks;
    private final HookState1122 hooks = new HookState1122();

    public EntityHost1122(EntityType<? extends EntityHost1122> type, Level level, Entity adopted) {
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == DATA && legacy != null && level().isClientSide()) DataSync1122.apply(legacy, entityData.get(DATA));
    }

    private void syncData() {
        if (legacy == null || level().isClientSide()) return;
        CompoundTag changed = DataSync1122.changed(legacy);
        if (changed != null) entityData.set(DATA, changed);
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
        syncData();
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
    protected net.minecraft.network.chat.Component getTypeName() {
        var name = Hosts1122.displayName(getType());
        return name != null ? name : super.getTypeName();
    }

    // ------------------------------------------------------------------ damage and interaction

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (legacy == null) return false;
        return legacy.attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource.mcc$of(source), amount);
    }

    @Override
    public boolean mcc$superHurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        markHurt();
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (legacy == null) return InteractionResult.PASS;
        return HookState1122.interact(legacy, player, hand, level().isClientSide());
    }

    @Override
    public InteractionResult interactAt(Player player, net.minecraft.world.phys.Vec3 vec, InteractionHand hand) {
        if (legacy == null) return InteractionResult.PASS;
        return HookState1122.interactAt(legacy, player, vec, hand);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (legacy != null) legacy.handleStatusUpdate(id);
        else super.handleEntityEvent(id);
    }

    @Override
    public void mcc$superHandleEntityEvent(byte id) {
        super.handleEntityEvent(id);
    }

    @Override
    public void push(net.minecraft.world.entity.Entity other) {
        if (legacy != null && Hosts1122.overrides(legacy.getClass(), "applyEntityCollision", Entity.class)) {
            var wrapped = EntityBridge1122.wrap(other);
            if (wrapped != null) legacy.applyEntityCollision(wrapped);
        } else {
            super.push(other);
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (legacy != null && EntityBridge1122.wrap(player) instanceof EntityPlayer p) legacy.onCollideWithPlayer(p);
    }

    @Override
    public boolean isPickable() {
        return legacy != null && legacy.canBeCollidedWith();
    }

    @Override
    public boolean isPushable() {
        return legacy != null && legacy.canBePushed();
    }

    @Override
    public boolean canBeCollidedWith() {
        if (legacy != null && Hosts1122.overrides(legacy.getClass(), "getCollisionBoundingBox")) return legacy.getCollisionBoundingBox() != null;
        return super.canBeCollidedWith();
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
    public void thunderHit(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LightningBolt bolt) {
        if (legacy == null) super.thunderHit(level, bolt);
        else legacy.onStruckByLightning(HookState1122.wrapBolt(bolt));
    }

    @Override
    public void mcc$superThunderHit(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LightningBolt bolt) {
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
    public boolean isPushedByFluid() {
        if (legacy != null && Hosts1122.overrides(legacy.getClass(), "isPushedByWater")) return legacy.isPushedByWater();
        return super.isPushedByFluid();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        if (legacy != null && Hosts1122.overrides(legacy.getClass(), "isInRangeToRenderDist", double.class)) return legacy.isInRangeToRenderDist(distance);
        return super.shouldRenderAtSqrDistance(distance);
    }

    // ------------------------------------------------------------------ size

    @Override
    public void mcc$setSize(float width, float height) {
        if (width == this.width && height == this.height) return;
        this.width = width;
        this.height = height;
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return width > 0 ? EntityDimensions.scalable(width, height) : super.getDimensions(pose);
    }

    // ------------------------------------------------------------------ saving and spawn data

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (legacy == null) return;
        try {
            legacy.mcc$readEntityFromNBT(new NBTTagCompound(tag));
            legacy.mcc$readCaps(new NBTTagCompound(tag));
        } catch (RuntimeException | LinkageError e) {
            Hosts1122.nbtFailure(this, "reading", e);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (legacy == null) return;
        try {
            legacy.mcc$writeEntityToNBT(new NBTTagCompound(tag));
            legacy.mcc$writeCaps(new NBTTagCompound(tag));
        } catch (RuntimeException | LinkageError e) {
            Hosts1122.nbtFailure(this, "writing", e);
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
