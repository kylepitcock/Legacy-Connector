package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;

/** 1.20.1's {@code SimpleCraftingRecipeSerializer.Factory}: makes a crafting recipe from its id and book category (1.21.1 recipes have no id). */
@FunctionalInterface
public interface LegacyCraftingFactory {
    CraftingRecipe create(ResourceLocation id, CraftingBookCategory category);
}
