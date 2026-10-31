package org.kylepitcock.legacyconnector.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * PACK COMPAT (Architectury): its event listener lists are plain ArrayLists, and mods register listeners from their parallel
 * client-setup threads while the render thread fires the client tick event, so loading crashed with a ConcurrentModificationException
 * in EventFactory about one launch in three in ATM10. The list becomes a {@link org.kylepitcock.legacyconnector.compat.architectury.ThreadSafeListenerList}. Optional: without
 * Architectury the target is missing and this mixin is skipped (legacyconnector.compat.mixins.json is not required).
 */
@Pseudo
@Mixin(targets = "dev.architectury.event.EventFactory$EventImpl", remap = false)
public abstract class ArchitecturyEventImplMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "java/util/ArrayList"), require = 0)
    private java.util.ArrayList<?> legacyconnector$threadSafeListeners() {
        return new org.kylepitcock.legacyconnector.compat.architectury.ThreadSafeListenerList<>();
    }
}
