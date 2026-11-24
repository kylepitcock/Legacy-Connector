package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 3 API: maps a mod's own recipe class to a category and a wrapper (JEI 4 replaced it with handleRecipes). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeHandler<T> {
    Class<T> getRecipeClass();
    String getRecipeCategoryUid(T recipe);
    IRecipeWrapper getRecipeWrapper(T recipe);
    boolean isRecipeValid(T recipe);
}
