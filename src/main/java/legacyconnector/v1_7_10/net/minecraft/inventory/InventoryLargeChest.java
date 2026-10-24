package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Two inventories presented as one (a double chest): the upper one's slots first. */
public class InventoryLargeChest implements IInventory {
    private final String name;
    private final IInventory upper;
    private final IInventory lower;

    public InventoryLargeChest(String name, IInventory upper, IInventory lower) {
        this.name = name;
        this.upper = upper == null ? lower : upper;
        this.lower = lower == null ? upper : lower;
    }

    @Override
    public int getSizeInventory() {
        return upper.getSizeInventory() + lower.getSizeInventory();
    }

    public boolean isPartOfLargeChest(IInventory inventory) {
        return upper == inventory || lower == inventory;
    }

    @Override
    public String getInventoryName() {
        return upper.hasCustomInventoryName() ? upper.getInventoryName() : lower.hasCustomInventoryName() ? lower.getInventoryName() : name;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return upper.hasCustomInventoryName() || lower.hasCustomInventoryName();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= upper.getSizeInventory() ? lower.getStackInSlot(slot - upper.getSizeInventory()) : upper.getStackInSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        return slot >= upper.getSizeInventory() ? lower.decrStackSize(slot - upper.getSizeInventory(), amount) : upper.decrStackSize(slot, amount);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return slot >= upper.getSizeInventory() ? lower.getStackInSlotOnClosing(slot - upper.getSizeInventory()) : upper.getStackInSlotOnClosing(slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot >= upper.getSizeInventory()) lower.setInventorySlotContents(slot - upper.getSizeInventory(), stack);
        else upper.setInventorySlotContents(slot, stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return upper.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        upper.markDirty();
        lower.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return upper.isUseableByPlayer(player) && lower.isUseableByPlayer(player);
    }

    @Override
    public void openInventory() {
        upper.openInventory();
        lower.openInventory();
    }

    @Override
    public void closeInventory() {
        upper.closeInventory();
        lower.closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }
}
