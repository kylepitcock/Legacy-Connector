package org.kylepitcock.legacyconnector.v1_7_10.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
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
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/** Host of a non-living legacy entity (projectiles, orbs, special items, ...). */
public class LegacyEntityHost extends net.minecraft.world.entity.Entity implements LegacyHost, IEntityWithComplexSpawn {
    static final EntityDataAccessor<CompoundTag> WATCHER = SynchedEntityData.defineId(LegacyEntityHost.class, EntityDataSerializers.COMPOUND_TAG);

    private Entity legacy;
    private float width = -1.0F;
    private float height = -1.0F;

    public LegacyEntityHost(EntityType<? extends LegacyEntityHost> type, Level level, Entity adopted) {
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
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
            if (LegacyHosts.tickFailed(this, legacy.getClass(), e, ++failedTicks)) discard();
        }
    }

    private int failedTicks;

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

    // ------------------------------------------------------------------ damage and interaction

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (legacy == null) return false;
        return legacy.attackEntityFrom(legacyconnector.v1_7_10.net.minecraft.util.DamageSource.mcc$wrap(source), amount);
    }

    @Override
    public boolean mcc$superHurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (legacy == null || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        var p = (legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer) EntityBridge.wrap(player);
        return legacy.interactFirst(p) ? InteractionResult.sidedSuccess(level().isClientSide()) : InteractionResult.PASS;
    }

    @Override
    public void playerTouch(Player player) {
        if (legacy != null) legacy.onCollideWithPlayer((legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer) EntityBridge.wrap(player));
    }

    @Override
    public boolean isPickable() {
        return legacy != null && legacy.canBeCollidedWith();
    }

    @Override
    public boolean isPushable() {
        return legacy != null && legacy.canBePushed();
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
    protected void addAdditionalSaveData(CompoundTag tag) {
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
