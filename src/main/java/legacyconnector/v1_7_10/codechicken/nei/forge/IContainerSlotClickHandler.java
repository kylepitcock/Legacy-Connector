package legacyconnector.v1_7_10.codechicken.nei.forge;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Slot;

/** NEI 1.4.7 / 1.5.2 slot click hook of container GUIs (kept by {@link GuiContainerManager#addSlotClickHandler}). */
public interface IContainerSlotClickHandler {
    default void beforeSlotClick(GuiContainer gui, int slotIndex, int button, Slot slot, int modifiers) {
    }

    default boolean handleSlotClick(GuiContainer gui, int slotIndex, int button, Slot slot, int modifiers, boolean eventConsumed) {
        return false;
    }

    default void afterSlotClick(GuiContainer gui, int slotIndex, int button, Slot slot, int modifiers) {
    }
}
