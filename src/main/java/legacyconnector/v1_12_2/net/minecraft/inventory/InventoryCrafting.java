package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** A crafting grid as a plain list of stacks. */
public class InventoryCrafting implements IInventory {
    private final NonNullList<ItemStack> stackList;
    private final int inventoryWidth;
    private final int inventoryHeight;

    public InventoryCrafting(Object eventHandler, int width, int height) {
        this.stackList = NonNullList.withSize(width * height, ItemStack.EMPTY);
        this.inventoryWidth = width;
        this.inventoryHeight = height;
    }

    @Override
    public int getSizeInventory() {
        return stackList.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : stackList) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index >= getSizeInventory() ? ItemStack.EMPTY : stackList.get(index);
    }

    public ItemStack getStackInRowAndColumn(int row, int column) {
        return row >= 0 && row < inventoryWidth && column >= 0 && column <= inventoryHeight ? getStackInSlot(row + column * inventoryWidth) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack s = stackList.get(index);
        return s.isEmpty() ? ItemStack.EMPTY : s.splitStack(count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack s = stackList.get(index);
        stackList.set(index, ItemStack.EMPTY);
        return s;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        stackList.set(index, stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        stackList.clear();
    }

    @Override
    public String getName() {
        return "container.crafting";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    public int getHeight() {
        return inventoryHeight;
    }

    public int getWidth() {
        return inventoryWidth;
    }
}
