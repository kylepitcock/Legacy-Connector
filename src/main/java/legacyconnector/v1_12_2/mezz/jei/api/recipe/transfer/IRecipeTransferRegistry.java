package legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer;

/** JEI 4 (Minecraft 1.12.2) API: recipe transfer (moving the ingredients into a machine; not bridged, recorded only). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeTransferRegistry {
    void addRecipeTransferHandler(Class<? extends legacyconnector.v1_12_2.net.minecraft.inventory.Container> containerClass, String recipeCategoryUid,
            int recipeSlotStart, int recipeSlotCount, int inventorySlotStart, int inventorySlotCount);
}
