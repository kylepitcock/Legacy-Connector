package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 (Minecraft 1.12.2) API: one recipe as shown. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeWrapper {
    void getIngredients(legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients ingredients);
    default void drawInfo(legacyconnector.v1_12_2.net.minecraft.client.Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {}
    default java.util.List<String> getTooltipStrings(int mouseX, int mouseY) {
        return java.util.Collections.emptyList();
    }
    default boolean handleClick(legacyconnector.v1_12_2.net.minecraft.client.Minecraft minecraft, int mouseX, int mouseY, int mouseButton) {
        return false;
    }
}
