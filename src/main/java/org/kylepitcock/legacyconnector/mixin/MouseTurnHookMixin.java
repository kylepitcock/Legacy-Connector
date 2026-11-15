package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.MouseHandler;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites.Client.MOUSE_OVERRIDE}): 1.7.10 coremods let a hook decide that the mouse no longer turns the player
 * (flight steering). A no-op without such a hook.
 */
@Mixin(MouseHandler.class)
public abstract class MouseTurnHookMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodMouseOverride(double movementTime, CallbackInfo ci) {
        if (CoremodSites.Client.MOUSE_OVERRIDE.listed() && CoremodSites.Client.mouseOverride()) ci.cancel();
    }
}
