package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.core.recipe.OversizedCraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only oversized legacy crafting results need more than one destination slot per craft. */
@Mixin(AbstractContainerMenu.class)
abstract class OversizedCraftingMenuMixin {
    @Shadow protected abstract boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse);
    @Shadow protected abstract void resetQuickCraft();
    @Unique private boolean mcc$continuingCraftTransfer;

    @Unique private ResultSlot mcc$oversizedResult() {
        var menu = (AbstractContainerMenu) (Object) this;
        if (!(menu instanceof CraftingMenu) && !(menu instanceof InventoryMenu)) return null;
        if (!(menu.getSlot(0) instanceof ResultSlot slot) || !(slot.container instanceof ResultContainer result)) return null;
        var recipe = result.getRecipeUsed();
        if (recipe == null) return null;
        return recipe.value() instanceof OversizedCraftingRecipe.Shaped || recipe.value() instanceof OversizedCraftingRecipe.Shapeless ? slot : null;
    }

    @Inject(method = "moveItemStackTo", at = @At("RETURN"), cancellable = true)
    private void mcc$fillRemainingCraftSlots(ItemStack stack, int start, int end, boolean reverse, CallbackInfoReturnable<Boolean> cir) {
        if (mcc$continuingCraftTransfer || stack.isEmpty()) return;
        var result = mcc$oversizedResult();
        if (result == null || result.getItem() != stack) return;
        boolean moved = cir.getReturnValue();
        mcc$continuingCraftTransfer = true;
        try {
            while (!stack.isEmpty()) {
                int before = stack.getCount();
                moved |= moveItemStackTo(stack, start, end, reverse);
                if (stack.getCount() >= before) break;
            }
        } finally {
            mcc$continuingCraftTransfer = false;
        }
        cir.setReturnValue(moved);
    }

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void mcc$splitCraftHotbarSwap(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
        if (type != ClickType.SWAP || slotId != 0 || !(button >= 0 && button < 9 || button == 40)) return;
        var result = mcc$oversizedResult();
        if (result == null || result.getItem().getCount() <= result.getItem().getMaxStackSize()) return;
        var inventory = player.getInventory();
        // An occupied destination cannot be swapped into a result slot; keep vanilla's no-op.
        if (!inventory.getItem(button).isEmpty()) return;
        resetQuickCraft();
        result.tryRemove(result.getItem().getCount(), Integer.MAX_VALUE, player).ifPresent(taken -> {
            var crafted = taken.copy();
            inventory.setItem(button, taken.split(Math.min(taken.getMaxStackSize(), inventory.getMaxStackSize())));
            if (!taken.isEmpty() && !inventory.add(taken)) player.drop(taken, true);
            // Native ResultSlot still owns achievements, ingredient consumption and remainders.
            result.onTake(player, crafted);
        });
        ci.cancel();
    }
}
