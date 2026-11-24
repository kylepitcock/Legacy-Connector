package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: the slots of one ingredient type. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IGuiIngredientGroup<T> {
    void set(legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients ingredients);
    void set(int slotIndex, java.util.List<T> ingredients);
    void set(int slotIndex, T ingredient);
    void setBackground(int slotIndex, IDrawable background);
    void addTooltipCallback(ITooltipCallback<T> tooltipCallback);
    java.util.Map<Integer, ? extends IGuiIngredient<T>> getGuiIngredients();
    void init(int slotIndex, boolean input, int xPosition, int yPosition);
    void init(int slotIndex, boolean input, legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRenderer<T> ingredientRenderer, int xPosition, int yPosition, int width, int height, int xInset, int yInset);
    void setOverrideDisplayFocus(legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<T> focus);
}
