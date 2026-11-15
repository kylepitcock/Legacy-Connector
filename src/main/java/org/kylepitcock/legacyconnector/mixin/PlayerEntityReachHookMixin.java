package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook site (generic, {@code CoremodSites.USE_ENTITY_REACH}): 1.7.10 coremods changed the squared distance within which the server lets a player
 * use or attack an entity. A no-op without such a hook.
 */
@Mixin(Player.class)
public abstract class PlayerEntityReachHookMixin {
    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodUseReach(CallbackInfoReturnable<Double> cir) {
        if (CoremodSites.USE_ENTITY_REACH.listed()) cir.setReturnValue(CoremodSites.useEntityReach((Player) (Object) this, cir.getReturnValue()));
    }
}
