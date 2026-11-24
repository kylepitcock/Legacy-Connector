package legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer;

/** JEI 4 API: where a container keeps a recipe's slots, for the basic transfer handler. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeTransferInfo<C extends legacyconnector.v1_12_2.net.minecraft.inventory.Container> {
    Class<C> getContainerClass();
    String getRecipeCategoryUid();
    boolean canHandle(C container);
    java.util.List<legacyconnector.v1_12_2.net.minecraft.inventory.Slot> getRecipeSlots(C container);
    java.util.List<legacyconnector.v1_12_2.net.minecraft.inventory.Slot> getInventorySlots(C container);
    default boolean requireCompleteSets() {
        return true;
    }
}
