package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.SMELT_RESULT}): 1.12.2 coremods put a hook after {@code TileEntityFurnace.smeltItem} looked up the
 * recipe's result; the hook answers the stack the furnace makes. A no-op without such a hook.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceResultHookMixin {
    @WrapOperation(method = "burn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/AbstractCookingRecipe;assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)"
                    + "Lnet/minecraft/world/item/ItemStack;"), require = 0)
    private static ItemStack legacyconnector$coremodSmeltResult(AbstractCookingRecipe recipe, SingleRecipeInput input, HolderLookup.Provider registries,
            Operation<ItemStack> original) {
        ItemStack result = original.call(recipe, input, registries);
        return CoremodSites1122.SMELT_RESULT.listed() ? CoremodSites1122.smeltResult(input.item(), result) : result;
    }
}
