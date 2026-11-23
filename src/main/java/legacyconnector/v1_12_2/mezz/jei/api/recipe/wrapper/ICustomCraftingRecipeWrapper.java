package legacyconnector.v1_12_2.mezz.jei.api.recipe.wrapper;

/** JEI 4 (Minecraft 1.12.2) API: a crafting recipe that lays out its own slots. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface ICustomCraftingRecipeWrapper extends ICraftingRecipeWrapper {
    void setRecipe(legacyconnector.v1_12_2.mezz.jei.api.gui.IRecipeLayout recipeLayout, legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients ingredients);
}
