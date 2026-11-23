package legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer;

/** JEI 4 (Minecraft 1.12.2) API: moves a shown recipe's ingredients into an open container (recorded; the modern JEI's transfer is not bridged). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeTransferHandler<C extends legacyconnector.v1_12_2.net.minecraft.inventory.Container> {
    Class<C> getContainerClass();

    /** JEI 4.0 to 4.7; later versions name the category at registration. */
    @Deprecated
    default String getRecipeCategoryUid() {
        return null;
    }

    IRecipeTransferError transferRecipe(C container, legacyconnector.v1_12_2.mezz.jei.api.gui.IRecipeLayout recipeLayout,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, boolean maxTransfer, boolean doTransfer);
}
