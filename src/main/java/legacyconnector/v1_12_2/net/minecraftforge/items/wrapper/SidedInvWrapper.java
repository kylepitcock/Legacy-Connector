package legacyconnector.v1_12_2.net.minecraftforge.items.wrapper;

import legacyconnector.v1_12_2.net.minecraft.inventory.ISidedInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandlerModifiable;
import legacyconnector.v1_12_2.net.minecraftforge.items.ItemHandlerHelper;

/** Forge's item handler over one side of a sided inventory: only that side's slots, with its insert and extract rules. */
public class SidedInvWrapper implements IItemHandlerModifiable {
    protected final ISidedInventory inv;
    protected final EnumFacing side;

    public SidedInvWrapper(ISidedInventory inv, EnumFacing side) {
        this.inv = inv;
        this.side = side;
    }

    public static int getSlot(ISidedInventory inv, int slot, EnumFacing side) {
        int[] slots = inv.getSlotsForFace(side);
        return slot < slots.length ? slots[slot] : -1;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SidedInvWrapper w && w.inv == inv && w.side == side;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(inv) * 31 + (side == null ? 0 : side.hashCode());
    }

    @Override
    public int getSlots() {
        return inv.getSlotsForFace(side).length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int i = getSlot(inv, slot, side);
        return i == -1 ? ItemStack.EMPTY : inv.getStackInSlot(i);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int slot1 = getSlot(inv, slot, side);
        if (slot1 == -1) return stack;
        ItemStack inSlot = inv.getStackInSlot(slot1);
        if (!inSlot.isEmpty()) {
            if (inSlot.getCount() >= Math.min(inSlot.getMaxStackSize(), getSlotLimit(slot))) return stack;
            if (!ItemHandlerHelper.canItemStacksStack(stack, inSlot)) return stack;
            if (!inv.canInsertItem(slot1, stack, side) || !inv.isItemValidForSlot(slot1, stack)) return stack;
            int m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - inSlot.getCount();
            if (stack.getCount() <= m) {
                if (!simulate) {
                    ItemStack copy = stack.copy();
                    copy.grow(inSlot.getCount());
                    setInventorySlotContents(slot1, copy);
                }
                return ItemStack.EMPTY;
            }
            stack = stack.copy();
            if (!simulate) {
                ItemStack copy = stack.splitStack(m);
                copy.grow(inSlot.getCount());
                setInventorySlotContents(slot1, copy);
            } else {
                stack.shrink(m);
            }
            return stack;
        }
        if (!inv.canInsertItem(slot1, stack, side) || !inv.isItemValidForSlot(slot1, stack)) return stack;
        int m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
        if (m < stack.getCount()) {
            stack = stack.copy();
            if (!simulate) setInventorySlotContents(slot1, stack.splitStack(m));
            else stack.shrink(m);
            return stack;
        }
        if (!simulate) setInventorySlotContents(slot1, stack);
        return ItemStack.EMPTY;
    }

    private void setInventorySlotContents(int slot, ItemStack stack) {
        inv.markDirty();
        inv.setInventorySlotContents(slot, stack);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        int slot1 = getSlot(inv, slot, side);
        if (slot1 == -1) return ItemStack.EMPTY;
        ItemStack inSlot = inv.getStackInSlot(slot1);
        if (inSlot.isEmpty() || !inv.canExtractItem(slot1, inSlot, side)) return ItemStack.EMPTY;
        if (simulate) {
            if (inSlot.getCount() < amount) return inSlot.copy();
            ItemStack copy = inSlot.copy();
            copy.setCount(amount);
            return copy;
        }
        int m = Math.min(inSlot.getCount(), amount);
        ItemStack ret = inv.decrStackSize(slot1, m);
        inv.markDirty();
        return ret;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        int slot1 = getSlot(inv, slot, side);
        if (slot1 != -1) setInventorySlotContents(slot1, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inv.getInventoryStackLimit();
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        int slot1 = getSlot(inv, slot, side);
        return slot1 == -1 ? false : inv.isItemValidForSlot(slot1, stack);
    }
}
