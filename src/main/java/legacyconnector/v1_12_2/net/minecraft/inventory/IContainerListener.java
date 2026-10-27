package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** Who sees a container (a player): told about every slot change and window property (progress bars). */
public interface IContainerListener {
    void sendAllContents(Container container, NonNullList<ItemStack> items);

    void sendSlotContents(Container container, int slotIndex, ItemStack stack);

    void sendWindowProperty(Container container, int varToUpdate, int newValue);

    void sendAllWindowProperties(Container container, IInventory inventory);
}
