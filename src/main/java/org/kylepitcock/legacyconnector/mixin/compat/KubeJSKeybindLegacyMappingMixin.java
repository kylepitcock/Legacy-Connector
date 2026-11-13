package org.kylepitcock.legacyconnector.mixin.compat;

import java.util.List;
import legacyconnector.v1_20_1.KubeJSKeybindCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a KubeJS 6-compatible path for direct KeyMapping construction to the
 * KubeJS 7 KeyBindEvents.registry event.
 */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.client.KeybindRegistryKubeEvent", remap = false)
public abstract class KubeJSKeybindLegacyMappingMixin {
    @Unique
    public Object registerLegacyMapping(String id, String translationKey, int defaultKey, String category) {
        return KubeJSKeybindCompat.registerLegacyMapping(this, id, translationKey, defaultKey, category);
    }

    @Inject(method = "build", at = @At("RETURN"), cancellable = true, remap = false)
    private void legacyconnector$appendLegacyMappings(CallbackInfoReturnable<List<?>> cir) {
        cir.setReturnValue(KubeJSKeybindCompat.appendLegacyMappings(this, cir.getReturnValue()));
    }
}
