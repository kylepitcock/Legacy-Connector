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

    void drawBackground(int recipe);

    void drawForeground(int recipe);

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
