package legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer;

/** JEI 4 (Minecraft 1.12.2) API: recipe transfer (moving the ingredients into a machine; not bridged, recorded only). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeTransferRegistry {
    void addRecipeTransferHandler(Class<? extends legacyconnector.v1_12_2.net.minecraft.inventory.Container> containerClass, String recipeCategoryUid,
            int recipeSlotStart, int recipeSlotCount, int inventorySlotStart, int inventorySlotCount);

    default void addRecipeTransferHandler(IRecipeTransferHandler<?> recipeTransferHandler, String recipeCategoryUid) {}

    /** JEI 4.0 to 4.7: the handler names its category. */
    @Deprecated
    default void addRecipeTransferHandler(IRecipeTransferHandler<?> recipeTransferHandler) {}

    default void addRecipeTransferHandler(IRecipeTransferInfo<?> recipeTransferInfo) {}

    default void addUniversalRecipeTransferHandler(IRecipeTransferHandler<?> recipeTransferHandler) {}
}
