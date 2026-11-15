package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.PLAYER_DROPS}): 1.12.2 coremods put a hook after the empty check of each stack in
 * {@code InventoryPlayer.dropAllItems}; a hook answering false keeps the stack in the inventory. A no-op without such a hook.
 */
@Mixin(Inventory.class)
public abstract class InventoryDropAllHookMixin {
    @Shadow
    @Final
    public Player player;

    @WrapOperation(method = "dropAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"), require = 0)
    private boolean legacyconnector$coremodPlayerDrops(ItemStack stack, Operation<Boolean> original, @Local(ordinal = 0) int slot) {
        boolean empty = original.call(stack);
        return empty || (CoremodSites1122.PLAYER_DROPS.listed() && !CoremodSites1122.playerDrops(player, slot, stack));
    }
}
