package org.kylepitcock.legacyconnector.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PACK COMPAT (Auroras by Verph, mod id "auroras"; not legacy code): the mod draws its auroras at the end of
 * {@code LevelRenderer.renderSky} (before the terrain, so the terrain covers them) unless {@code AHelpers.areShadersEnabled()}
 * says Iris runs a shader pack; then it draws them at the end of {@code LevelRenderer.renderLevel}, after the terrain, with
 * whatever depth test state the last render type left (vanilla render types switch the depth test off when they finish). That
 * check reads Iris's {@code enableShaders} option, not whether a pack is in use: with the option on and no valid pack (ATM10:
 * "Shaders are disabled because no valid shaderpack is selected") the End's green/purple aurora band was drawn across the
 * terrain in front of the camera. The check now answers whether Iris really renders with a pack ({@code IrisApi.isShaderPackInUse}),
 * so without one the aurora is drawn in the sky pass as the mod intends. {@code compat.auroras.irisPackCheck=false} turns it off.
 */
@Pseudo
@Mixin(targets = "auroras.util.AHelpers", remap = false)
public abstract class AurorasIrisStateMixin {
    @Inject(method = "areShadersEnabled()Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void legacyconnector$packInUse(CallbackInfoReturnable<Boolean> cir) {
        if (!org.kylepitcock.legacyconnector.core.LegacyConfig.aurorasIrisPackCheck()) return;
        Boolean inUse = org.kylepitcock.legacyconnector.compat.IrisState.shaderPackInUse();
        if (inUse != null) cir.setReturnValue(inUse);
    }
}
