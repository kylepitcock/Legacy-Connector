package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MOD-SPECIFIC COMPAT (Fisk's Superheroes): its coremod skipped the attack and the held-button block breaking while a shield is up or
 * the hero's ability sits on the left mouse button ({@code ASMHooksClient.disableLeftClick}). See
 * {@code compat.fiskheroes.FiskHeroesClient}; a no-op without the mod.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$fiskLeftClick(CallbackInfoReturnable<Boolean> cir) {
        if (((Minecraft) (Object) this).player != null && org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesClient.disableLeftClick()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$fiskHeldLeftClick(boolean leftClick, CallbackInfo ci) {
        if (leftClick && ((Minecraft) (Object) this).player != null
                && org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesClient.disableLeftClick()) {
            ci.cancel();
        }
    }
}
