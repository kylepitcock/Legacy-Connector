package legacyconnector.v1_7_10.codechicken.nei;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** NEI's layout of the overlay on the GUI; empty here (JEI lays out its own). */
public class LayoutManager {
    private static final LayoutManager INSTANCE = new LayoutManager();

    public static ItemPanel itemPanel = new ItemPanel();

    public static LayoutManager instance() {
        return INSTANCE;
    }

    public ItemStack getStackUnderMouse(GuiContainer gui, int mousex, int mousey) {
        return null;
    }

    public static boolean isHidden() {
        return true;
    }

    public Widget getInputFocused() {
        return null;
    }
}
