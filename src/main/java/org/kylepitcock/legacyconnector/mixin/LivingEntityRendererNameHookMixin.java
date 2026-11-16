package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.NAME_SHOWN}): 1.12.2 coremods put a hook first in {@code RenderLivingBase.canRenderName} that can
 * suppress the name tag of an entity. A no-op without such a hook.
 */
@CoremodHook
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererNameHookMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodNameShown(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (CoremodSites1122.NAME_SHOWN.listed() && cir.getReturnValueZ() && !CoremodSites1122.answers(CoremodSites1122.NAME_SHOWN, entity)) cir.setReturnValue(false);
    }
}
