package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.CAKE_EATEN}): 1.12.2 coremods that rework food put a hook in place of the hunger gain of a cake
 * bite (the hook adds the hunger itself, after its own events). A no-op without such a hook; the game's gain stays when the hook fails.
 */
@Mixin(CakeBlock.class)
public abstract class CakeEatHookMixin {
    @WrapOperation(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"), require = 0)
    private static void legacyconnector$coremodCakeEaten(FoodData data, int food, float saturation, Operation<Void> original, LevelAccessor level, BlockPos pos,
            BlockState state, Player player) {
        if (CoremodSites1122.CAKE_EATEN.listed() && CoremodSites1122.cakeEaten(level, state, player)) return;
        original.call(data, food, saturation);
    }
}
