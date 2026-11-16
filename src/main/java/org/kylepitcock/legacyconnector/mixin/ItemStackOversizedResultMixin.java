package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * 1.20.1 read a data stack ({@code {"item":..,"count":8}} of a recipe or loot entry) without checking the count against the item's maximum stack size;
 * 1.21.1's strict stack codec rejects it and the whole recipe is lost (RFTools Base's crafting card: count 8 on an item that stacks to 1). Counts that
 * are valid in vanilla pass either way, so the check here only stops rejecting what the legacy format accepted (the upper bound of 99 stays).
 */
@Mixin(ItemStack.class)
public abstract class ItemStackOversizedResultMixin {
    @WrapOperation(method = "validateStrict", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private static int legacyconnector$maxStackForData(ItemStack stack, Operation<Integer> original) {
        return Math.max(original.call(stack), 99);
    }
}
