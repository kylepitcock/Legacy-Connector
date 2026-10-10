package mccompat.v1_7_10.net.minecraft.inventory;

import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.inventory.CraftingContainer;

/** View over a modern crafting grid, handed to custom legacy IRecipe implementations. */
public class InventoryCrafting implements IInventory {
    private final CraftingContainer mcc$grid;

    public InventoryCrafting(CraftingContainer grid) {
        this.mcc$grid = grid;
    }

    @Override
    public int getSizeInventory() {
        return mcc$grid.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= getSizeInventory() ? null : ItemStack.mcc$wrap(mcc$grid.getItem(slot));
    }

    public ItemStack getStackInRowAndColumn(int row, int column) {
        if (row < 0 || row >= mcc$grid.getWidth() || column < 0 || column > mcc$grid.getHeight()) return null;
        return getStackInSlot(row + column * mcc$grid.getWidth());
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        return ItemStack.mcc$wrap(mcc$grid.removeItem(slot, amount));
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return ItemStack.mcc$wrap(mcc$grid.removeItemNoUpdate(slot));
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        mcc$grid.setItem(slot, ItemStack.mcc$toModern(stack));
    }

    @Override
    public String getInventoryName() {
        return "container.crafting";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        mcc$grid.setChanged();
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
