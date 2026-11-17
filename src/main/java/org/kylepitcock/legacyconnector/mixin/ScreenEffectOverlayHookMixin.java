package org.kylepitcock.legacyconnector.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.kylepitcock.legacyconnector.v1_7_10.client.CoremodClientSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.Client.SCREEN_OVERLAYS}): 1.7.10 coremods drew their own liquid overlays at the start of
 * {@code ItemRenderer.renderOverlays}. A no-op without such a hook.
 */
@CoremodHook
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectOverlayHookMixin {
    @Inject(method = "renderScreenEffect", at = @At("HEAD"), require = 0)
    private static void legacyconnector$coremodScreenOverlays(Minecraft mc, PoseStack pose, CallbackInfo ci) {
        if (CoremodSites.Client.SCREEN_OVERLAYS.listed()) CoremodClientSites.screenOverlays(pose);
    }
}
