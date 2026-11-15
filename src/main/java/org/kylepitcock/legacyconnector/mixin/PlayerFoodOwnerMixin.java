package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells {@link CoremodState} which player a food data object belongs to. Legacy coremods that rework hunger add a player field to {@code FoodStats} and fill
 * it through a constructor they add; here the game's own constructor records the pair, so that field reads as the player.
 */
@Mixin(Player.class)
public abstract class PlayerFoodOwnerMixin {
    @Shadow
    protected FoodData foodData;

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void legacyconnector$foodOwner(CallbackInfo ci) {
        CoremodState.owner(foodData, this);
    }
}
