package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;

/** 1.20.1's {@code SimpleCookingSerializer.CookieBaker}: the recipe constructor of a cooking recipe type, taking the recipe id first. */
@FunctionalInterface
public interface LegacyCookieBaker {
    AbstractCookingRecipe create(ResourceLocation id, String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime);
}
