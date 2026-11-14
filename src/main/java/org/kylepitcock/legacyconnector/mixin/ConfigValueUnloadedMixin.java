package org.kylepitcock.legacyconnector.mixin;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge's {@code ConfigValue.get()} answered the default while its config was not loaded; NeoForge's throws "Cannot get config value before config is
 * loaded". Legacy mods read values in places that run before (or without) the load: a {@code ModConfigEvent} handler that reads the client spec while
 * the server config loads on a dedicated server, static initialisers, registration. The default is returned and not cached, but only for a value read by
 * a translated legacy mod: native mods keep NeoForge's exception. The stack is only walked on the unloaded path and the verdict is kept per value.
 */
@Mixin(targets = "net.neoforged.neoforge.common.ModConfigSpec$ConfigValue")
public abstract class ConfigValueUnloadedMixin<T> {
    @Shadow
    private ModConfigSpec spec;
    @Shadow
    @Final
    private Supplier<T> defaultSupplier;

    @Unique
    private Boolean legacyconnector$legacyReader;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$defaultWhenUnloaded(CallbackInfoReturnable<T> cir) {
        if (spec == null || spec.isLoaded()) return;
        Boolean legacy = legacyconnector$legacyReader;
        if (legacy == null) legacyconnector$legacyReader = legacy = org.kylepitcock.legacyconnector.util.LegacyCallers.calledFromTranslatedJar();
        if (legacy) cir.setReturnValue(defaultSupplier.get());
    }
}
