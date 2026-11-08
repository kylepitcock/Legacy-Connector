package legacyconnector.vb1_7_3.net.minecraft.screen.slot;

import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Beta 1.7.3 {@code Slot}: one place of an inventory at a screen position. */
public class Slot {
    public final int index;
    public final Inventory inventory;
    public int id;
    public int x;
    public int y;

    public Slot(Inventory inventory, int index, int x, int y) {
        this.inventory = inventory;
        this.index = index;
        this.x = x;
        this.y = y;
    }

    /** MCP {@code onPickupFromSlot}. */
    public void onTakeItem(ItemStack stack) {
        markDirty();
    }

    /** MCP {@code isItemValid}. */
    public boolean canInsert(ItemStack stack) {
        return true;
    }

    public ItemStack getStack() {
        return inventory.getStack(index);
    }

    public boolean hasStack() {
        return getStack() != null;
    }

    public void setStack(ItemStack stack) {
        inventory.setStack(index, stack);
        markDirty();
    }

    public void markDirty() {
        inventory.markDirty();
    }

    public int getMaxItemCount() {
        return inventory.getMaxCountPerStack();
    }

    public int getBackgroundTextureId() {
        return -1;
    }

    public ItemStack takeStack(int amount) {
        return inventory.removeStack(index, amount);
    }

    public boolean equals(Inventory inventory, int index) {
        return inventory == this.inventory && index == this.index;
    }
}
