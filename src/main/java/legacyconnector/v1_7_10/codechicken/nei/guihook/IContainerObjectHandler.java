package legacyconnector.v1_7_10.codechicken.nei.guihook;

import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Tells NEI which stack a GUI object (not a slot) is showing under the cursor. */
public interface IContainerObjectHandler {
    void guiTick(GuiContainer gui);

    void refresh(GuiContainer gui);

    void load(GuiContainer gui);

    ItemStack getStackUnderMouse(GuiContainer gui, int mousex, int mousey);

    boolean objectUnderMouse(GuiContainer gui, int mousex, int mousey);

    boolean shouldShowTooltip(GuiContainer gui);
}
