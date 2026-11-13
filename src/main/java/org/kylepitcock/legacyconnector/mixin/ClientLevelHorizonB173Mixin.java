package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The sky draws a black plane under the horizon (y 63 unless the level is flat). A Beta dimension whose provider reports no
 * ground (a sky realm) has no void below its islands to hide, so its horizon is the bottom of the world and the plane never shows.
 */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel$ClientLevelData")
public abstract class ClientLevelHorizonB173Mixin {
    @Inject(method = "getHorizonHeight", at = @At("HEAD"), cancellable = true)
    private void legacyconnector$noVoidPlane(LevelHeightAccessor level, CallbackInfoReturnable<Double> cir) {
        if (level instanceof ClientLevel client
                && client.effects() instanceof org.kylepitcock.legacyconnector.vb1_7_3.client.DimensionEffectsB173 effects
                && !effects.hasGround())
            cir.setReturnValue((double) level.getMinBuildHeight());
    }
}
