package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: the slots of one shown recipe. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeLayout {
    IGuiItemStackGroup getItemStacks();
    IGuiFluidStackGroup getFluidStacks();
    <T> IGuiIngredientGroup<T> getIngredientsGroup(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType);
    <T> IGuiIngredientGroup<T> getIngredientsGroup(Class<T> ingredientClass);
    legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<?> getFocus();
    void setRecipeTransferButton(int posX, int posY);
    void setShapeless();
}
