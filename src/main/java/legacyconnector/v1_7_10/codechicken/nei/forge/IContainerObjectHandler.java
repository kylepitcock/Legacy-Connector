package legacyconnector.v1_7_10.codechicken.nei.forge;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** NEI 1.4.7 / 1.5.2 hook for objects drawn over a container GUI (kept by {@link GuiContainerManager#addObjectHandler}). */
public interface IContainerObjectHandler {
    default void guiTick(GuiContainer gui) {
    }

    default void refresh(GuiContainer gui) {
    }

    default void load(GuiContainer gui) {
    }

    default ItemStack getStackUnderMouse(GuiContainer gui, int mousex, int mousey) {
        return null;
    }

    default boolean objectUnderMouse(GuiContainer gui, int mousex, int mousey) {
        return false;
    }

    default boolean shouldShowTooltip(GuiContainer gui) {
        return true;
    }
}
