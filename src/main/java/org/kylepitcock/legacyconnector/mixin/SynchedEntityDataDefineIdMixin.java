package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Remembers every entity data id handed out, so {@code build()} can define one that its reserver never defined (see {@link LegacyEntityData}). */
@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataDefineIdMixin {
    @Inject(method = "defineId", at = @At("RETURN"), require = 0)
    private static <T> void legacyconnector$record(Class<? extends SyncedDataHolder> owner, EntityDataSerializer<T> serializer,
            CallbackInfoReturnable<EntityDataAccessor<T>> cir) {
        LegacyEntityData.record(owner, cir.getReturnValue());
    }
}
