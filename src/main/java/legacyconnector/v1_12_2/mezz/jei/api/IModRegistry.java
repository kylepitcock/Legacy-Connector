package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: what a plugin registers. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IModRegistry {
    legacyconnector.v1_12_2.mezz.jei.api.IJeiHelpers getJeiHelpers();
    legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRegistry getIngredientRegistry();
    void addRecipes(java.util.Collection<?> recipes, String recipeCategoryUid);
    <T> void handleRecipes(Class<T> recipeClass, legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapperFactory<T> recipeWrapperFactory, String recipeCategoryUid);
    void addRecipeClickArea(Class<? extends legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer> guiContainerClass, int xPos, int yPos, int width, int height, String... recipeCategoryUids);
    void addRecipeCatalyst(Object catalystIngredient, String... recipeCategoryUids);
    <T> void addIngredientInfo(T ingredient, legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType, String... descriptionKeys);
    <T> void addIngredientInfo(java.util.List<T> ingredients, legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<T> ingredientType, String... descriptionKeys);
    <T> void addIngredientInfo(T ingredient, Class<? extends T> ingredientClass, String... descriptionKeys);
    <T> void addIngredientInfo(java.util.List<T> ingredients, Class<? extends T> ingredientClass, String... descriptionKeys);
    void addAdvancedGuiHandlers(legacyconnector.v1_12_2.mezz.jei.api.gui.IAdvancedGuiHandler<?>... advancedGuiHandlers);
    legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferRegistry getRecipeTransferRegistry();
    /** JEI 3 name of ingredient info. */
    @Deprecated
    default void addDescription(legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStack, String... descriptionKeys) {
        addIngredientInfo(itemStack, legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class, descriptionKeys);
    }
    @Deprecated
    default void addDescription(java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> itemStacks, String... descriptionKeys) {
        addIngredientInfo(itemStacks, legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class, descriptionKeys);
    }
    /** JEI 3 name of a catalyst. */
    default void addRecipeCategoryCraftingItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack craftingItem, String... recipeCategoryUids) {
        addRecipeCatalyst(craftingItem, recipeCategoryUids);
    }
}
