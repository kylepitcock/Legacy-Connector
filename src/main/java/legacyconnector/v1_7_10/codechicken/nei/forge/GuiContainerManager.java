package legacyconnector.v1_7_10.codechicken.nei.forge;

import java.awt.Point;
import legacyconnector.v1_7_10.codechicken.nei.recipe.TemplateRecipeHandler;

/**
 * NEI 1.4.7's GUI drawing hub: the recipe handlers of that era draw their machine pictures, progress bars, energy gauges and text through the
 * manager passed to {@code drawExtras(GuiContainerManager, int)} (Thermal Expansion's handlers). Each call goes to the drawing the 1.7.10 NEI
 * handlers use (the recipe area's coordinates, the texture bound by the handler).
 */
public class GuiContainerManager {
    /** The one manager the recipe drawing hands to handlers. */
    public static final GuiContainerManager INSTANCE = new GuiContainerManager();

    /** NEI 1.4.7: binds a texture by its path ({@code "/mods/thermalexpansion/gfx/gui_smelter.png"}). */
    public void bindTextureByName(String path) {
        TemplateRecipeHandler.mcc$changeTexture(path);
    }

    /** A region of the bound 256x256 texture at (x, y). */
    public void drawTexturedModalRect(int x, int y, int u, int v, int w, int h) {
        TemplateRecipeHandler.mcc$drawTexturedModalRect(x, y, u, v, w, h);
    }

    /** Text at (x, y) in a colour (0xRRGGBB), with the shadow vanilla's labels have when asked. */
    public void drawText(int x, int y, String text, int colour, boolean shadow) {
        if (text == null) return;
        legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().fontRenderer.drawString(text, x, y, colour, shadow);
    }

    public void drawText(int x, int y, String text, int colour) {
        drawText(x, y, text, colour, true);
    }

    /** The mouse in GUI coordinates. */
    public Point getMousePosition() {
        return legacyconnector.v1_7_10.codechicken.lib.gui.GuiDraw.getMousePosition();
    }
}
