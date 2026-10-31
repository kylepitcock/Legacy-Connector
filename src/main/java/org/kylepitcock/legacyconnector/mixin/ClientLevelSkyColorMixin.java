package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MOD-SPECIFIC COMPAT (DragonAPI): the sky colour goes through DragonAPI's SkyColorEvent, which its coremod posted from
 * {@code ForgeHooksClient.getSkyBlendColour} (VoidMonster darkens the sky with the fog). See
 * {@code compat.dragonapi.DragonApiClientEvents}; a no-op without DragonAPI.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelSkyColorMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void legacyconnector$dragonApiSkyColor(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Vec3 color = cir.getReturnValue();
        // a mod dimension's legacy provider that overrides getSkyColor decides the sky colour
        Vec3 fromProvider = org.kylepitcock.legacyconnector.v1_7_10.client.LegacyDimensionEffects.skyColor((ClientLevel) (Object) this, color, partialTick);
        if (fromProvider != color) {
            cir.setReturnValue(fromProvider);
            return;
        }
        Vec3 out = org.kylepitcock.legacyconnector.v1_7_10.compat.dragonapi.DragonApiClientEvents.skyColor(color);
        if (out != color) cir.setReturnValue(out);
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void legacyconnector$providerStarBrightness(float partialTick, CallbackInfoReturnable<Float> cir) {
        float current = cir.getReturnValue();
        float out = org.kylepitcock.legacyconnector.v1_7_10.client.LegacyDimensionEffects.starBrightness((ClientLevel) (Object) this, current, partialTick);
        if (out != current) cir.setReturnValue(out);
    }
}
