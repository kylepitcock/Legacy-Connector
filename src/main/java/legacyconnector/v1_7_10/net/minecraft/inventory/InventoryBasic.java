package legacyconnector.v1_7_10.net.minecraft.inventory;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A plain inventory with a fixed number of slots. */
public class InventoryBasic implements IInventory {
    private String inventoryTitle;
    private final int slotsCount;
    private final ItemStack[] inventoryContents;
    private List<IInvBasic> field_70480_d;
    private boolean field_94051_e;

    public InventoryBasic(String title, boolean customName, int slots) {
        this.inventoryTitle = title;
        this.field_94051_e = customName;
        this.slotsCount = slots;
        this.inventoryContents = new ItemStack[slots];
    }

    public void func_110134_a(IInvBasic listener) {
        if (field_70480_d == null) field_70480_d = new ArrayList<>();
        field_70480_d.add(listener);
    }

    public void func_110132_b(IInvBasic listener) {
        if (field_70480_d != null) field_70480_d.remove(listener);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < inventoryContents.length ? inventoryContents[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (inventoryContents[slot] == null) return null;
        ItemStack out;
        if (inventoryContents[slot].mcc$count() <= amount) {
            out = inventoryContents[slot];
            inventoryContents[slot] = null;
        } else {
            out = inventoryContents[slot].splitStack(amount);
            if (inventoryContents[slot].mcc$count() == 0) inventoryContents[slot] = null;
        }
        markDirty();
        return out;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (inventoryContents[slot] == null) return null;
        ItemStack out = inventoryContents[slot];
        inventoryContents[slot] = null;
        return out;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventoryContents[slot] = stack;
        if (stack != null && stack.mcc$count() > getInventoryStackLimit()) stack.mcc$setCount(getInventoryStackLimit());
        markDirty();
    }

    @Override
    public int getSizeInventory() {
        return slotsCount;
    }

    @Override
    public String getInventoryName() {
        return inventoryTitle;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return field_94051_e;
    }

    public void func_110133_a(String title) {
        this.field_94051_e = true;
        this.inventoryTitle = title;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        if (field_70480_d != null) for (IInvBasic l : new ArrayList<>(field_70480_d)) l.onInventoryChanged(this);
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
