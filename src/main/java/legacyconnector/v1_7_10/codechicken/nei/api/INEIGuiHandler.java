package legacyconnector.v1_7_10.codechicken.nei.api;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * NEI's per-GUI hook (item panel layout, slots hidden under a GUI, drag and drop). JEI lays out its own panel, so handlers are
 * accepted and never asked; the type exists because mods subclass {@link INEIGuiAdapter} and register the instance (DragonAPI's
 * TooltipOccluder: with a synthesized adapter that did not implement this interface, its NEI config failed verification and
 * DragonAPI with every Reika mod failed to load).
 */
public interface INEIGuiHandler {
    default Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return null;
    }

    default boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {
        return false;
    }

    default boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return false;
    }
}
