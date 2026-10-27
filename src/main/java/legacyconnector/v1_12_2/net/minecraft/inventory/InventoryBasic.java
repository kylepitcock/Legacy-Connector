package legacyconnector.v1_12_2.net.minecraft.inventory;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** A plain inventory of a fixed number of slots with a title and change listeners. */
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

    public InventoryBasic(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent title, int slotCount) {
        this(title.getUnformattedText(), true, slotCount);
    }

    public void addInventoryChangeListener(IInventoryChangedListener listener) {
        if (changeListeners == null) changeListeners = new java.util.ArrayList<>();
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
        ItemStack out = ItemStackHelper.getAndSplit(inventoryContents, index, count);
        if (!out.isEmpty()) markDirty();
        return out;
    }

    /** Adds a stack, onto matching stacks first, then into empty slots; what did not fit is returned. */
    public ItemStack addItem(ItemStack stack) {
        ItemStack itemstack = stack.copy();
        for (int i = 0; i < slotsCount; i++) {
            ItemStack here = getStackInSlot(i);
            if (here.isEmpty()) {
                setInventorySlotContents(i, itemstack);
                markDirty();
                return ItemStack.EMPTY;
            }
            if (ItemStack.areItemsEqual(here, itemstack)) {
                int j = Math.min(getInventoryStackLimit(), here.getMaxStackSize());
                int k = Math.min(itemstack.getCount(), j - here.getCount());
                if (k > 0) {
                    here.grow(k);
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
        ItemStack s = getStackInSlot(index);
        if (s.isEmpty()) return ItemStack.EMPTY;
        inventoryContents.set(index, ItemStack.EMPTY);
        return s;
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
        for (ItemStack s : inventoryContents) if (!s.isEmpty()) return false;
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

    public void setCustomName(String name) {
        this.hasCustomName = true;
        this.inventoryTitle = name;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        return hasCustomName ? new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(getName())
                : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation(getName());
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
