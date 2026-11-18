package legacyconnector.v1_7_10.codechicken.nei.forge;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * NEI 1.4.7 / 1.5.2 tooltip hook of container GUIs (the 1.7.10 shape is {@code guihook.IContainerTooltipHandler}). Handlers registered through
 * {@link GuiContainerManager#addTooltipHandler} are kept in its list; the JEI bridge draws no NEI panel, so nothing calls them.
 */
public interface IContainerTooltipHandler {
    default List<String> handleTooltipFirst(GuiContainer gui, int mousex, int mousey, List<String> currenttip) {
        return currenttip;
    }

    default List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, List<String> currenttip) {
        return currenttip;
    }
}
