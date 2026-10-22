package mccompat.v1_7_10.net.minecraft.inventory;

import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;

/** The single output slot of a crafting grid. */
public class InventoryCraftResult implements IInventory {
    private final ItemStack[] stackResult = new ItemStack[1];

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return stackResult[0];
    }

    @Override
    public String getInventoryName() {
        return "Result";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (stackResult[0] == null) return null;
        ItemStack out = stackResult[0];
        stackResult[0] = null;
        return out;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (stackResult[0] == null) return null;
        ItemStack out = stackResult[0];
        stackResult[0] = null;
        return out;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        stackResult[0] = stack;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }
}
