package legacyconnector.v1_12_2.baubles.api.cap;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The Baubles slots of a pack without Curios (what the Baubles bridge sits on): seven slots that never hold anything. */
public final class EmptyBaublesHandler implements IBaublesItemHandler {
    public static final EmptyBaublesHandler INSTANCE = new EmptyBaublesHandler();

    private EmptyBaublesHandler() {}

    public int getSlots() { return 7; }

    public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }

    public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }

    public int getSlotLimit(int slot) { return 1; }

    public boolean isItemValid(int slot, ItemStack stack) { return false; }

    public void setStackInSlot(int slot, ItemStack stack) { }

    public boolean isItemValidForSlot(int slot, ItemStack stack, EntityLivingBase player) { return false; }

    public boolean isEventBlocked() { return false; }

    public void setEventBlock(boolean blockEvents) { }

    public boolean isChanged(int slot) { return false; }

    public void setChanged(int slot, boolean changed) { }

    public void setPlayer(EntityLivingBase player) { }
}
