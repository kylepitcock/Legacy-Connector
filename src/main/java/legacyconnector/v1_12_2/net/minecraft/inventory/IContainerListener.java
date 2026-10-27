package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** Something watching a container: the server player it is open for (slot contents and window properties go to its client). */
public interface IContainerListener {
    void sendAllContents(Container container, NonNullList<ItemStack> items);

    void sendSlotContents(Container container, int slot, ItemStack stack);

    void sendWindowProperty(Container container, int id, int value);

    void sendAllWindowProperties(Container container, IInventory inventory);
}
