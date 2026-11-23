package legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer;

/** JEI 4 (Minecraft 1.12.2) API: why a recipe cannot be moved into a container. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeTransferError {
    enum Type {
        INTERNAL, USER_FACING, COSMETIC
    }

    Type getType();

    default void showError(legacyconnector.v1_12_2.net.minecraft.client.Minecraft minecraft, int mouseX, int mouseY,
            legacyconnector.v1_12_2.mezz.jei.api.gui.IRecipeLayout recipeLayout, int recipeX, int recipeY) {}
}
