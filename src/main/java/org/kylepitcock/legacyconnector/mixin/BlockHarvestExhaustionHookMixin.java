package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.EXHAUSTION_HARVEST}): the exhaustion of breaking a block ({@code Block.harvestBlock}, 0.005) goes
 * through the hooks of mods that rework hunger. A no-op without such a hook.
 */
@Mixin(Block.class)
public abstract class BlockHarvestExhaustionHookMixin {
    @WrapOperation(method = "playerDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"), require = 0)
    private void legacyconnector$coremodHarvestExhaustion(Player player, float amount, Operation<Void> original) {
        original.call(player, CoremodSites1122.EXHAUSTION_HARVEST.listed() ? CoremodSites1122.exhaustion(CoremodSites1122.EXHAUSTION_HARVEST, player, amount) : amount);
    }
}
