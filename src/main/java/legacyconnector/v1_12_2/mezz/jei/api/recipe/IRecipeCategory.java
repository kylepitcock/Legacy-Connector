package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 (Minecraft 1.12.2) API: a recipe category. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeCategory<T extends IRecipeWrapper> {
    String getUid();
    String getTitle();
    String getModName();
    legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawable getBackground();
    default legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawable getIcon() {
        return null;
    }
    default void drawExtras(legacyconnector.v1_12_2.net.minecraft.client.Minecraft minecraft) {}
    void setRecipe(legacyconnector.v1_12_2.mezz.jei.api.gui.IRecipeLayout recipeLayout, T recipeWrapper, legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients ingredients);
    default java.util.List<String> getTooltipStrings(int mouseX, int mouseY) {
        return java.util.Collections.emptyList();
    }
}
