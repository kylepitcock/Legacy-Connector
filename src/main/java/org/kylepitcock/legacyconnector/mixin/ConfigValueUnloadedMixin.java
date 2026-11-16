package org.kylepitcock.legacyconnector.mixin;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge 1.20.1 let a mod read a config value whose file is not loaded yet (a server config read while the data packs load for a new world, a default that
 * is computed from another value of the same spec); NeoForge throws. The read answers the value's own default and is not cached, so the loaded value is
 * read once the config is there.
 */
@Mixin(ModConfigSpec.ConfigValue.class)
public abstract class ConfigValueUnloadedMixin<T> {
    @Shadow
    private Supplier<T> defaultSupplier;

    @Shadow
    private ModConfigSpec spec;

    @Inject(method = "get()Ljava/lang/Object;", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$unloadedDefault(CallbackInfoReturnable<T> cir) {
        if (spec != null && !spec.isLoaded()) cir.setReturnValue(defaultSupplier.get());
    }
}
