package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.MigrateContext;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.OverridableBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1 created an entity's data holder before calling {@code defineSynchedData()}, so a mod could call {@code setNoGravity(true)} or {@code setSilent(true)} from it.
 * In 1.21.1 the holder is built from the builder after {@code defineSynchedData(Builder)}: those setters hit a null holder. While a bridged legacy
 * {@code defineSynchedData} is running (its builder is remembered in {@link MigrateContext}) they replace the default of the id in the builder instead.
 */
@Mixin(Entity.class)
public abstract class EntityEarlyDataMixin {
    @Shadow
    protected SynchedEntityData entityData;
    @Shadow
    @Final
    private static EntityDataAccessor<Boolean> DATA_NO_GRAVITY;
    @Shadow
    @Final
    private static EntityDataAccessor<Boolean> DATA_SILENT;
    @Shadow
    @Final
    private static EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE;

    @Inject(method = "setNoGravity", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$earlyNoGravity(boolean value, CallbackInfo ci) {
        early(DATA_NO_GRAVITY, value, ci);
    }

    @Inject(method = "setSilent", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$earlySilent(boolean value, CallbackInfo ci) {
        early(DATA_SILENT, value, ci);
    }

    @Inject(method = "setCustomNameVisible", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$earlyNameVisible(boolean value, CallbackInfo ci) {
        early(DATA_CUSTOM_NAME_VISIBLE, value, ci);
    }

    private void early(EntityDataAccessor<Boolean> accessor, boolean value, CallbackInfo ci) {
        if (entityData != null) return;
        SynchedEntityData.Builder b = MigrateContext.currentBuilder();
        if (b instanceof OverridableBuilder o) {
            o.legacyconnector$overrideDefault(accessor, value);
            ci.cancel();
        }
    }
}
