package org.kylepitcock.legacyconnector.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PACK COMPAT (Create Aeronautics' Rocketnautics, in cosmonautics-*.jar): its deep-space sky (DeepSpaceHandler.renderUniverse, drawn
 * every frame even with enableCustomSky=false) rebuilds the star plasma texture pixel by pixel with layered warped noise on every game
 * tick; that was 11-12% of the render thread in ATM10. The texture is rebuilt every {@code compat.rocketnautics.plasmaEveryTicks}
 * ticks instead (default 4: its slow drift updates five times a second; 1 restores the original). Optional and client only.
 */
@Pseudo
@Mixin(targets = "dev.devce.rocketnautics.client.SkyHandler", remap = false)
public abstract class RocketnauticsStarPlasmaMixin {
    @Shadow(remap = false)
    private static net.minecraft.resources.ResourceLocation STAR_PLASMA_TEXTURE_ID;

    @Inject(method = "ensureStarPlasmaTexture()V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void legacyconnector$throttlePlasma(CallbackInfo ci) {
        int every = org.kylepitcock.legacyconnector.core.LegacyConfig.rocketnauticsPlasmaEveryTicks();
        if (every <= 1 || STAR_PLASMA_TEXTURE_ID == null) return; // the first build always runs
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && level.getGameTime() % every != 0) ci.cancel();
    }
}
