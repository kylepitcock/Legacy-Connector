package legacyconnector.v1_12_2.net.minecraft.inventory;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;

/** A plain inventory of a fixed size, with a name and change listeners (backpacks, temporary grids). */
public class InventoryBasic implements IInventory {
    private String inventoryTitle;
    private final int slotsCount;
    private final NonNullList<ItemStack> inventoryContents;
    private List<IInventoryChangedListener> changeListeners;
    private boolean hasCustomName;

    public InventoryBasic(String title, boolean customName, int slotCount) {
        this.inventoryTitle = title;
        this.hasCustomName = customName;
        this.slotsCount = slotCount;
        this.inventoryContents = NonNullList.withSize(slotCount, ItemStack.EMPTY);
    }

    public InventoryBasic(ITextComponent title, int slotCount) {
        this(title.getUnformattedText(), true, slotCount);
    }

    public void addInventoryChangeListener(IInventoryChangedListener listener) {
        if (changeListeners == null) changeListeners = new ArrayList<>();
        changeListeners.add(listener);
    }

    public void removeInventoryChangeListener(IInventoryChangedListener listener) {
        if (changeListeners != null) changeListeners.remove(listener);
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < inventoryContents.size() ? inventoryContents.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack itemstack = ItemStackHelper.getAndSplit(inventoryContents, index, count);
        if (!itemstack.isEmpty()) markDirty();
        return itemstack;
    }

    /** Merges the stack into matching and then empty slots; returns what did not fit. */
    public ItemStack addItem(ItemStack stack) {
        ItemStack itemstack = stack.copy();
        for (int i = 0; i < slotsCount; ++i) {
            ItemStack itemstack1 = getStackInSlot(i);
            if (itemstack1.isEmpty()) {
                setInventorySlotContents(i, itemstack);
                markDirty();
                return ItemStack.EMPTY;
            }
            if (ItemStack.areItemsEqual(itemstack1, itemstack)) {
                int j = Math.min(getInventoryStackLimit(), itemstack1.getMaxStackSize());
                int k = Math.min(itemstack.getCount(), j - itemstack1.getCount());
                if (k > 0) {
                    itemstack1.grow(k);
                    itemstack.shrink(k);
                    if (itemstack.isEmpty()) {
                        markDirty();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        if (itemstack.getCount() != stack.getCount()) markDirty();
        return itemstack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack itemstack = inventoryContents.get(index);
        if (itemstack.isEmpty()) return ItemStack.EMPTY;
        inventoryContents.set(index, ItemStack.EMPTY);
        return itemstack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inventoryContents.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
        markDirty();
    }

    @Override
    public int getSizeInventory() {
        return slotsCount;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : inventoryContents) if (!itemstack.isEmpty()) return false;
        return true;
    }

    @Override
    public String getName() {
        return inventoryTitle;
    }

    @Override
    public boolean hasCustomName() {
        return hasCustomName;
    }

    public void setCustomName(String inventoryTitleIn) {
        this.hasCustomName = true;
        this.inventoryTitle = inventoryTitleIn;
    }

    @Override
    public ITextComponent getDisplayName() {
        return hasCustomName() ? new TextComponentString(getName()) : new TextComponentTranslation(getName());
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        if (changeListeners != null) for (IInventoryChangedListener l : changeListeners) l.onInventoryChanged(this);
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
        inventoryContents.clear();
    }
}
