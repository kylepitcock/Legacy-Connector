package legacyconnector.v1_12_2.net.minecraftforge.items.wrapper;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandlerModifiable;

/** An item handler with no slots. */
public class EmptyHandler implements IItemHandlerModifiable {
    public static final IItemHandler INSTANCE = new EmptyHandler();

    @Override
    public int getSlots() {
        return 0;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
    }

    @Override
    public int getSlotLimit(int slot) {
        return 0;
    }
}
