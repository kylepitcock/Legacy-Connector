package org.kylepitcock.legacyconnector.compat.nei;

import java.awt.Point;
import legacyconnector.v1_7_10.codechicken.nei.forge.GuiContainerManager;
import legacyconnector.v1_7_10.codechicken.nei.recipe.GuiRecipe;
import legacyconnector.v1_7_10.codechicken.nei.recipe.TemplateRecipeHandler;

/**
 * The stand-in for NEI's recipe screen that a handler gets in {@code handleTooltip(gui, tip, recipe)}: JEI draws the recipe, so this holds the
 * recipe's own frame. The screen is as wide and high as its GUI (no margin, so {@code (width - xSize) / 2} is 0 as the 1.4.7 / 1.5.2 handlers
 * compute their screen offset), the recipe's origin is NEI's (5, 16), and {@code manager.getMousePosition()} is the mouse in that frame:
 * the position JEI reports inside the recipe plus the origin.
 */
public final class RecipeScreen extends GuiRecipe {
    private static final Point ORIGIN = new Point(5, 16);

    public RecipeScreen(TemplateRecipeHandler handler, int recipeX, int recipeY) {
        super();
        this.xSize = 176;
        this.ySize = 166;
        this.width = 176;
        this.height = 166;
        this.mcc$manager = GuiContainerManager.withMouse(this, new Point(recipeX + ORIGIN.x, recipeY + ORIGIN.y));
        this.currenthandlers.add(handler);
    }

    @Override
    public Point getRecipePosition(int recipe) {
        return new Point(ORIGIN);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    }
}
