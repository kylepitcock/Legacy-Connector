package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

public interface IItemHandler {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    /** Forge 14.23: whether the slot accepts the stack at all (handlers written before it accept everything). */
    default boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
