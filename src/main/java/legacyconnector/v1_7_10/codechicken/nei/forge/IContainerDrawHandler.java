package legacyconnector.v1_7_10.codechicken.nei.forge;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Slot;

/** NEI 1.4.7 / 1.5.2 drawing hook of container GUIs (kept by {@link GuiContainerManager#addDrawHandler}; nothing calls it under JEI). */
public interface IContainerDrawHandler {
    default void onPreDraw(GuiContainer gui) {
    }

    default void renderObjects(GuiContainer gui, int mousex, int mousey) {
    }

    default void postRenderObjects(GuiContainer gui, int mousex, int mousey) {
    }

    default void renderSlotUnderlay(GuiContainer gui, Slot slot) {
    }

    default void renderSlotOverlay(GuiContainer gui, Slot slot) {
    }
}
