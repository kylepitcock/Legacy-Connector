package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Parrot;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/** The texture half of the shoulder parrot hook ({@code CoremodSites1122.SHOULDER_TEXTURE}); see {@link ShoulderParrotTextureHookMixin}. */
@CoremodHook
@Mixin(ParrotRenderer.class)
public abstract class ParrotTextureHookMixin {
    @Inject(method = "getVariantTexture", at = @At("RETURN"), cancellable = true, require = 0)
    private static void legacyconnector$coremodShoulderTexture(Parrot.Variant variant, CallbackInfoReturnable<ResourceLocation> cir) {
        CompoundTag tag = CoremodSites1122.SHOULDER_TAG.get();
        if (tag == null || !CoremodSites1122.SHOULDER_TEXTURE.listed()) return;
        ResourceLocation changed = CoremodSites1122.shoulderTexture(cir.getReturnValue(), tag);
        if (!changed.equals(cir.getReturnValue())) cir.setReturnValue(changed);
    }
}
