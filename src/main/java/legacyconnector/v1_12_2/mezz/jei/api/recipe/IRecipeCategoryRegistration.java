package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 (Minecraft 1.12.2) API: category registration. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeCategoryRegistration {
    void addRecipeCategories(IRecipeCategory<?>... recipeCategories);
    legacyconnector.v1_12_2.mezz.jei.api.IJeiHelpers getJeiHelpers();
}
