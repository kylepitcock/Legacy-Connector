package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** Watches a container (the player who has it open is one: {@code EntityPlayerMP}). */
public interface IContainerListener {
    void sendAllContents(Container container, NonNullList<ItemStack> items);

    void sendSlotContents(Container container, int slot, ItemStack stack);

    /** A progress bar value (furnace burn time, machine energy): arrives in the client container's {@code updateProgressBar}. */
    void sendWindowProperty(Container container, int id, int value);

    void sendAllWindowProperties(Container container, IInventory inventory);
}
