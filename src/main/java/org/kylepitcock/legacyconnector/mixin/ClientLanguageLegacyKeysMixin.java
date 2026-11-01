package org.kylepitcock.legacyconnector.mixin;

import java.util.Map;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyLang;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A key the loaded languages miss is looked up in the legacy translation sources ({@link LegacyLang}): translations mods
 * register from code after the resource reload, and 1.7.10 vanilla keys ({@code tile.stone.name}). Only misses pay.
 */
@Mixin(ClientLanguage.class)
public abstract class ClientLanguageLegacyKeysMixin {
    @Shadow
    @Final
    private Map<String, String> storage;

    @Inject(method = "getOrDefault", at = @At("RETURN"), cancellable = true)
    private void legacyconnector$legacyText(String key, String defaultValue, CallbackInfoReturnable<String> cir) {
        // a hit returns the stored string, never the caller's default object: only misses go further
        if (key == null || cir.getReturnValue() != defaultValue || storage.containsKey(key)) return;
        String legacy = LegacyLang.lookup((Language) (Object) this, key);
        if (legacy != null) cir.setReturnValue(legacy);
    }

    @Inject(method = "has", at = @At("RETURN"), cancellable = true)
    private void legacyconnector$legacyKey(String key, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && LegacyLang.lookup((Language) (Object) this, key) != null) cir.setReturnValue(true);
    }
}
