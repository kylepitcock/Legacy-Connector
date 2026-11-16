package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The root registry knows the lazy view of the registries (enchantment, potion...) that translated 1.20.1 code holds as statics: the root registry names them by their key. */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryLazyViewMixin {
    @Inject(method = "getKey(Ljava/lang/Object;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$lazyViewKey(Object value, CallbackInfoReturnable<ResourceLocation> cir) {
        // the views of registries that moved or turned dynamic are proxies that answer key()
        if (value != null && java.lang.reflect.Proxy.isProxyClass(value.getClass()) && value instanceof net.minecraft.core.Registry<?> view) cir.setReturnValue(view.key().location());
    }
}
