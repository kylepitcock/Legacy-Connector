package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

public interface IInventory {
    int getSizeInventory();

    ItemStack getStackInSlot(int slot);

    ItemStack decrStackSize(int slot, int amount);

    ItemStack getStackInSlotOnClosing(int slot);

    void setInventorySlotContents(int slot, ItemStack stack);

    String getInventoryName();

    boolean hasCustomInventoryName();

    int getInventoryStackLimit();

    void markDirty();

    boolean isUseableByPlayer(EntityPlayer player);

    void openInventory();

    void closeInventory();

    boolean isItemValidForSlot(int slot, ItemStack stack);
}
