package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook sites (generic, {@code CoremodSites1122.FOOD_*} and {@code EXHAUSTION_ADDED/CAP}): 1.12.2 coremods that rework hunger (the 20 food points,
 * the 40 exhaustion, the tick) put hooks into {@code FoodStats}; the shim's food stats are views of this class, so the hooks run here. No-ops without hooks.
 */
@Mixin(FoodData.class)
public abstract class FoodDataHookMixin1122 {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodFoodTick(Player player, CallbackInfo ci) {
        if (CoremodSites1122.FOOD_TICK.listed() && CoremodSites1122.foodTick((FoodData) (Object) this, player)) ci.cancel();
    }

    @Inject(method = "needsFood", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodFoodNeeded(CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites1122.FOOD_NEEDED.listed()) return;
        boolean value = cir.getReturnValueZ();
        boolean changed = CoremodSites1122.foodNeeded((FoodData) (Object) this, value);
        if (changed != value) cir.setReturnValue(changed);
    }

    @ModifyConstant(method = "add", constant = @Constant(intValue = 20), require = 0)
    private int legacyconnector$coremodFoodMax(int max) {
        return CoremodSites1122.FOOD_MAX.listed() ? CoremodSites1122.foodMax((FoodData) (Object) this, max) : max;
    }

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true, require = 0)
    private float legacyconnector$coremodExhaustionAdded(float amount) {
        return CoremodSites1122.EXHAUSTION_ADDED.listed() ? CoremodSites1122.exhaustionAdded((FoodData) (Object) this, amount) : amount;
    }

    @ModifyConstant(method = "addExhaustion", constant = @Constant(floatValue = 40.0F), require = 0)
    private float legacyconnector$coremodExhaustionCap(float cap) {
        return CoremodSites1122.EXHAUSTION_CAP.listed() ? CoremodSites1122.exhaustionCap((FoodData) (Object) this, cap) : cap;
    }
}
