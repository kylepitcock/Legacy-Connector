package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook site (generic, {@code CoremodSites.Client.BLOCK_DESTRUCTION}): 1.7.10 coremods put a hook first in
 * {@code PlayerControllerMP.onPlayerDestroyBlock} that leaves the block alone (no creative instant breaking while a weapon is out). Without the hook list:
 * MOD-SPECIFIC COMPAT (Fisk's Superheroes): the same hook ({@code ASMHooksClient.disableBlockDestruction}), see {@code compat.fiskheroes.FiskHeroesClient};
 * a no-op without the mod.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$fiskNoBlockBreaking(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (CoremodSites.Client.BLOCK_DESTRUCTION.listed()) {
            if (CoremodSites.Client.blockDestruction()) cir.setReturnValue(false);
        } else if (player.getAbilities().instabuild
                && org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesClient.disableBlockDestruction()) {
            cir.setReturnValue(false);
        }
    }
}
