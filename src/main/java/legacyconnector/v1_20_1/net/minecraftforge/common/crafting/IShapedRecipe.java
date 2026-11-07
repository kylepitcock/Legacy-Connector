package legacyconnector.v1_20_1.net.minecraftforge.common.crafting;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

/** Forge 1.20.1's marker for shaped recipes (recipe viewers read the size); NeoForge 1.21.1 reads {@code ShapedRecipe} directly. */
public interface IShapedRecipe<T extends RecipeInput> extends Recipe<T> {
    int getRecipeWidth();

    int getRecipeHeight();
}
