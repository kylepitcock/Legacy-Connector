package legacyconnector.v1_12_2.mezz.jei.transfer;

/** JEI 4 (Minecraft 1.12.2) internals some plugins reach into; here a small working copy. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
/** JEI's basic handler. The bridge reads the info out of it and lets the modern JEI's own basic transfer do the work. */
public class BasicRecipeTransferHandler<C extends legacyconnector.v1_12_2.net.minecraft.inventory.Container> implements legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandler<C> {
    private final legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper handlerHelper;
    private final legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferInfo<C> transferHelper;

    public BasicRecipeTransferHandler(legacyconnector.v1_12_2.mezz.jei.startup.StackHelper stackHelper, legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper handlerHelper, legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferInfo<C> transferHelper) {
        this.handlerHelper = handlerHelper;
        this.transferHelper = transferHelper;
    }

    public legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferInfo<C> info() {
        return transferHelper;
    }

    @Override
    public Class<C> getContainerClass() {
        return transferHelper.getContainerClass();
    }

    @Override
    public String getRecipeCategoryUid() {
        return transferHelper.getRecipeCategoryUid();
    }

    /** Never called by the bridge (it runs the modern transfer instead); a caller that does gets an internal error. */
    @Override
    public legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferError transferRecipe(C container, legacyconnector.v1_12_2.mezz.jei.api.gui.IRecipeLayout recipeLayout, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        return handlerHelper.createInternalError();
    }
}
