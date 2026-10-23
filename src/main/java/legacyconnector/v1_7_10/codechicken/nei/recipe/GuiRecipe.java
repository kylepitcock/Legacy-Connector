package legacyconnector.v1_7_10.codechicken.nei.recipe;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;

/** NEI's recipe screen. Under JEI there is none; handlers receive this type only in signatures (it is a container screen, as in NEI). */
public abstract class GuiRecipe extends GuiContainer {
    protected GuiRecipe() {
        super(null);
    }
}
