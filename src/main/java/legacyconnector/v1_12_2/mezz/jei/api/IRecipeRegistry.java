package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: recipe lookups (focus creation). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeRegistry {
    <V> legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<V> createFocus(legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus.Mode mode, V ingredient);
    /** The wrappers a category shows. */
    <T extends legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper> java.util.List<T> getRecipeWrappers(legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeCategory<T> recipeCategory);
    /** Shows one more recipe in a category while the game runs. */
    void addRecipe(legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper recipe, String recipeCategoryUid);
    /** Hides a recipe of a category while the game runs. */
    void removeRecipe(legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper recipe, String recipeCategoryUid);
    default void hideRecipe(legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper recipe, String recipeCategoryUid) {
        removeRecipe(recipe, recipeCategoryUid);
    }
}
