package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.util.List;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;
import legacyconnector.v1_7_10.codechicken.nei.api.IOverlayHandler;
import legacyconnector.v1_7_10.codechicken.nei.api.IRecipeOverlayRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

public interface IRecipeHandler {
    String getRecipeName();

    int numRecipes();

    /** NEI 1.6/1.7 shape. A handler of the 1.4.7/1.5.2 shape ({@link #drawBackground(legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager, int)}) is reached through here. */
    default void drawBackground(int recipe) {
        drawBackground(legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager.INSTANCE, recipe);
    }

    default void drawForeground(int recipe) {
        drawForeground(legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager.INSTANCE, recipe);
    }

    /** NEI 1.4.7 / 1.5.2 shape: the handler draws through the manager it is handed ({@link TemplateRecipeHandler} has NEI's defaults). */
    default void drawBackground(legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager manager, int recipe) {
    }

    default void drawForeground(legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager manager, int recipe) {
    }

    List<PositionedStack> getIngredientStacks(int recipe);

    List<PositionedStack> getOtherStacks(int recipe);

    PositionedStack getResultStack(int recipe);

    void onUpdate();

    boolean hasOverlay(GuiContainer gui, Container container, int recipe);

    IRecipeOverlayRenderer getOverlayRenderer(GuiContainer gui, int recipe);

    IOverlayHandler getOverlayHandler(GuiContainer gui, int recipe);

    int recipiesPerPage();

    List<String> handleTooltip(GuiRecipe gui, List<String> tip, int recipe);

    List<String> handleItemTooltip(GuiRecipe gui, ItemStack stack, List<String> tip, int recipe);

    boolean keyTyped(GuiRecipe gui, char c, int key, int recipe);

    boolean mouseClicked(GuiRecipe gui, int button, int recipe);
}
