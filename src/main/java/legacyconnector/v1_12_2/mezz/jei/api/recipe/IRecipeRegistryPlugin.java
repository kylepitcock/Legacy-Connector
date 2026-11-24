package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 API: a plugin answering recipe lookups itself (recorded only: the modern JEI has its own lookup). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeRegistryPlugin {
    <V> java.util.List<String> getRecipeCategoryUids(IFocus<V> focus);
    <T extends IRecipeWrapper, V> java.util.List<T> getRecipeWrappers(IRecipeCategory<T> recipeCategory, IFocus<V> focus);
    <T extends IRecipeWrapper> java.util.List<T> getRecipeWrappers(IRecipeCategory<T> recipeCategory);
}
