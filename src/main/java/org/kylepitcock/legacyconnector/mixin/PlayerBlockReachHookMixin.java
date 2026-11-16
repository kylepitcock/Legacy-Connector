package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.Client.BLOCK_REACH}): 1.7.10 coremods changed the distance at which the client reaches blocks
 * ({@code PlayerControllerMP.getBlockReachDistance}). Client players only. A no-op without such a hook.
 */
@CoremodHook
@Mixin(Player.class)
public abstract class PlayerBlockReachHookMixin {
    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodBlockReach(CallbackInfoReturnable<Double> cir) {
        if (CoremodSites.Client.BLOCK_REACH.listed() && ((Player) (Object) this).level().isClientSide) cir.setReturnValue(CoremodSites.Client.blockReach(cir.getReturnValue()));
    }
}
