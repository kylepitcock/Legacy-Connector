package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/** Coremod hook site (generic, {@code CoremodSites1122.EXHAUSTION_HUNGER_EFFECT}): the exhaustion of the hunger effect goes through the hunger hooks. */
@CoremodHook
@Mixin(targets = "net.minecraft.world.effect.HungerMobEffect")
public abstract class HungerEffectExhaustionHookMixin {
    @WrapOperation(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"), require = 0)
    private void legacyconnector$coremodHungerEffect(Player player, float amount, Operation<Void> original) {
        original.call(player, CoremodSites1122.EXHAUSTION_HUNGER_EFFECT.listed()
                ? CoremodSites1122.exhaustion(CoremodSites1122.EXHAUSTION_HUNGER_EFFECT, player, amount) : amount);
    }
}
