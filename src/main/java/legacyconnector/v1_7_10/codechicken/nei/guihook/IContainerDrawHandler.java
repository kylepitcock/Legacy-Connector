package legacyconnector.v1_7_10.codechicken.nei.guihook;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;

/** Draw hooks of container GUIs. */
public interface IContainerDrawHandler {
    void onPreDraw(GuiContainer gui);

    void renderObjects(GuiContainer gui, int mousex, int mousey);

    void postRenderObjects(GuiContainer gui, int mousex, int mousey);
}
