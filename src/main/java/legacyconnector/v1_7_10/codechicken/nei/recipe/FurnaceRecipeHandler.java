package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.awt.Rectangle;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;

/**
 * NEI's smelting handler. Mods subclass it for furnace-like machines (Traincraft's distillation tower and open hearth furnace) and
 * fill {@link #arecipes} themselves; the vanilla smelting list is JEI's own.
 */
public class FurnaceRecipeHandler extends TemplateRecipeHandler {
    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(50, 23, 18, 18), "fuel"));
        transferRects.add(new RecipeTransferRect(new Rectangle(74, 23, 24, 18), "smelting"));
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return null;
    }

    @Override
    public String getRecipeName() {
        return "Smelting";
    }

    @Override
    public String getGuiTexture() {
        return "textures/gui/container/furnace.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "smelting";
    }
}
