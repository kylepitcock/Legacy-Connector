package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: the recipe screen. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipesGui {
    <V> void show(legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<V> focus);
    void showCategories(java.util.List<String> recipeCategoryUids);
    default Object getIngredientUnderMouse() {
        return null;
    }
}
