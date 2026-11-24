package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * 1.20.1's {@code SingleItemRecipe.Serializer.SingleItemMaker} (the recipe factory a stonecutter-like serializer was built with). 1.21.1 dropped the recipe id
 * from the factory ({@code SingleItemRecipe.Factory}); {@link Round16Compat#singleItemFactory} adapts a maker to it.
 */
@FunctionalInterface
public interface LegacySingleItemMaker<T extends SingleItemRecipe> {
    T create(ResourceLocation id, String group, Ingredient ingredient, ItemStack result);
}
