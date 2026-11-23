package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: helpers. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IJeiHelpers {
    legacyconnector.v1_12_2.mezz.jei.api.IGuiHelper getGuiHelper();
    legacyconnector.v1_12_2.mezz.jei.api.recipe.IStackHelper getStackHelper();
    legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientBlacklist getIngredientBlacklist();
    legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper getRecipeTransferHandlerHelper();
}
