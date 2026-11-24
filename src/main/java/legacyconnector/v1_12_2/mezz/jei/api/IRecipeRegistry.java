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
    /** JEI 4: the wrapper of a mod's recipe object, found through the handler it registered (null if none takes it). */
    legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper getRecipeWrapper(Object recipe, String recipeCategoryUid);
    /** JEI 3: a recipe object of a mod, found through its handler. */
    void addRecipe(Object recipe);
    void removeRecipe(Object recipe);
    java.util.List<legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeCategory> getRecipeCategories(java.util.List<String> recipeCategoryUids);
    java.util.List<legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeCategory> getRecipeCategories();
    legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandler getRecipeTransferHandler(legacyconnector.v1_12_2.net.minecraft.inventory.Container container, legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeCategory recipeCategory);
}
