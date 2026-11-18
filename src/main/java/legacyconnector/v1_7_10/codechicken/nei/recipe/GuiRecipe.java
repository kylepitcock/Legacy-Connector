package legacyconnector.v1_7_10.codechicken.nei.recipe;

import legacyconnector.v1_7_10.codechicken.nei.forge.IContainerClientSide;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;

/**
 * NEI's recipe screen. Under JEI there is none; handlers receive this type in {@code handleTooltip}, {@code handleItemTooltip},
 * {@code keyTyped} and {@code mouseClicked}, and 1.4.7 / 1.5.2 handlers read its {@code manager} (the drawing hub with the mouse) and the
 * screen's {@code width}, {@code xSize}, {@code height} and {@code ySize} to turn the mouse into the screen's frame. The layer's JEI
 * category hands them a stand-in that holds the recipe's own frame ({@link #getRecipePosition} is the recipe's origin).
 */
public abstract class GuiRecipe extends GuiContainer implements IContainerClientSide {
    protected GuiRecipe() {
        super(null);
    }

    /** NEI: the screen the recipe screen was opened from. */
    protected GuiRecipe(GuiContainer prevgui) {
        super(null);
        this.prevGui = prevgui;
        this.firstGui = prevgui;
    }

    /** The recipe handlers of the open screen (there is no NEI recipe screen under JEI: the stand-in lists the one handler it is for). */
    public java.util.ArrayList<ICraftingHandler> currenthandlers = new java.util.ArrayList<>();
    /** 0 for crafting, 1 for usage recipes. */
    public int recipetype;
    public int page;
    public GuiContainer firstGui;
    public GuiContainer prevGui;

    public GuiContainer getFirstScreen() {
        return firstGui;
    }

    public java.util.ArrayList<ICraftingHandler> getCurrentRecipeHandlers() {
        return currenthandlers;
    }

    public void refreshPage() {
    }

    /** The top left corner of the given recipe slot of the screen (NEI stacked them 65 pixels apart below the title). */
    public java.awt.Point getRecipePosition(int recipe) {
        return new java.awt.Point(5, 16 + 65 * Math.max(0, recipe));
    }

    /** True when the mouse is over the given recipe slot (never: there is no NEI recipe screen under JEI). */
    public boolean isMouseOver(legacyconnector.v1_7_10.codechicken.nei.PositionedStack stack, int recipe) {
        return false;
    }
}
