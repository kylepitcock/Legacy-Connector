package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyEntityData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Defines the entity data ids a legacy mod reserved on a vanilla class but never defined through the builder (see {@link LegacyEntityData}). */
@Mixin(SynchedEntityData.Builder.class)
public abstract class SynchedEntityDataBuilderMixin {
    @Shadow
    @Final
    private SyncedDataHolder entity;

    @Shadow
    @Final
    private SynchedEntityData.DataItem<?>[] itemsById;

    @Inject(method = "build", at = @At("HEAD"), require = 0)
    private void legacyconnector$fillHoles(CallbackInfoReturnable<SynchedEntityData> cir) {
        LegacyEntityData.fillHoles(entity, itemsById, (SynchedEntityData.Builder) (Object) this);
    }
}
