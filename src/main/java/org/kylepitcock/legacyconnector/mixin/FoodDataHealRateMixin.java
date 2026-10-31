package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MOD-SPECIFIC COMPAT (Fisk's Superheroes): its coremod asked {@code ASMHooks.getHealRate} for the number of ticks between natural
 * heals (healing factors heal faster, some powers not at all). See {@code compat.fiskheroes.FiskHeroesHooks}; a no-op without the mod.
 */
@Mixin(FoodData.class)
public abstract class FoodDataHealRateMixin {
    @Unique
    private Player legacyconnector$player;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void legacyconnector$rememberPlayer(Player player, CallbackInfo ci) {
        legacyconnector$player = player;
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 80), require = 0)
    private int legacyconnector$slowHealRate(int rate) {
        return legacyconnector$player == null ? rate : org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesHooks.healRate(legacyconnector$player, rate);
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 10), require = 0)
    private int legacyconnector$fastHealRate(int rate) {
        if (legacyconnector$player == null) return rate;
        // the saturated heal runs 8 times as often as the 80-tick one
        int scaled = org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesHooks.healRate(legacyconnector$player, 80);
        return scaled == 80 ? rate : (scaled == Integer.MAX_VALUE ? scaled : Math.max(1, scaled / 8));
    }
}
