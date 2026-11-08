package legacyconnector.v1_7_10.codechicken.nei.recipe;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;

/** NEI's recipe screen. Under JEI there is none; handlers receive this type only in signatures (it is a container screen, as in NEI). */
public abstract class GuiRecipe extends GuiContainer {
    protected GuiRecipe() {
        super(null);
    }

    /** True when the mouse is over the given recipe slot (never: there is no NEI recipe screen under JEI). */
    public boolean isMouseOver(legacyconnector.v1_7_10.codechicken.nei.PositionedStack stack, int recipe) {
        return false;
    }

    /** The screen position of a recipe in the page (none under JEI: the origin). */
    public java.awt.Point getRecipePosition(int recipe) {
        return new java.awt.Point(0, 0);
    }
}
