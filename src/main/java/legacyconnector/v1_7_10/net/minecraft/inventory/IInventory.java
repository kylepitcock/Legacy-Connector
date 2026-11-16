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

    /** Default for jars of older versions, whose inventories had no such method (1.4.7: Thermal Expansion's tiles). */
    default boolean hasCustomInventoryName() {
        return false;
    }

    int getInventoryStackLimit();

    void markDirty();

    boolean isUseableByPlayer(EntityPlayer player);

    void openInventory();

    void closeInventory();

    /** Default for jars of versions before 1.5 (no per-slot validity: every stack fits, as their slots decided). */
    default boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }
}
