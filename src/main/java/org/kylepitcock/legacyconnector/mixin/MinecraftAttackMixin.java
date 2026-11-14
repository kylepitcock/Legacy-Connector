package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook sites (generic, {@code CoremodSites.Client.LEFT_CLICK}, {@code RIGHT_CLICK_DELAY}): 1.7.10 coremods put a cancelling hook first in
 * {@code Minecraft.clickMouse} and {@code sendClickBlockToController} (no attack or block breaking while a shield is up or a power sits on the
 * button) and changed the delay between right clicks. Without the hook list (no 1.7.10 client jar to read the coremods against):
 * MOD-SPECIFIC COMPAT (Fisk's Superheroes): the same left click hook, see {@code compat.fiskheroes.FiskHeroesClient}; a no-op without the mod.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$fiskLeftClick(CallbackInfoReturnable<Boolean> cir) {
        if (((Minecraft) (Object) this).player == null) return;
        if (CoremodSites.Client.LEFT_CLICK.listed() ? CoremodSites.Client.leftClick()
                : org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesClient.disableLeftClick()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$fiskHeldLeftClick(boolean leftClick, CallbackInfo ci) {
        if (!leftClick || ((Minecraft) (Object) this).player == null) return;
        if (CoremodSites.Client.LEFT_CLICK.listed() ? CoremodSites.Client.leftClick()
                : org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesClient.disableLeftClick()) {
            ci.cancel();
        }
    }

    @ModifyConstant(method = "startUseItem", constant = @Constant(intValue = 4), require = 0)
    private int legacyconnector$coremodRightClickDelay(int ticks) {
        return CoremodSites.Client.RIGHT_CLICK_DELAY.listed() ? CoremodSites.Client.rightClickDelay(ticks) : ticks;
    }
}
