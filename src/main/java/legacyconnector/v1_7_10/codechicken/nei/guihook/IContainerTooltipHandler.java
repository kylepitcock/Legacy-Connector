package legacyconnector.v1_7_10.codechicken.nei.guihook;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Adds lines to tooltips in container GUIs. */
public interface IContainerTooltipHandler {
    List<String> handleTooltip(GuiContainer gui, int mousex, int mousey, List<String> currenttip);

    List<String> handleItemDisplayName(GuiContainer gui, ItemStack itemstack, List<String> currenttip);

    List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, int mousex, int mousey, List<String> currenttip);
}
