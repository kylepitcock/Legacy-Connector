package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MOD-SPECIFIC COMPAT (Fisk's Superheroes): its coremod stopped creative-mode instant block breaking while a weapon or ability is out
 * ({@code ASMHooksClient.disableBlockDestruction}). See {@code compat.fiskheroes.FiskHeroesClient}; a no-op without the mod.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$fiskNoBlockBreaking(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getAbilities().instabuild
                && org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesClient.disableBlockDestruction()) {
            cir.setReturnValue(false);
        }
    }
}
