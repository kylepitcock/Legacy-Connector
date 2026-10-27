package legacyconnector.v1_12_2.net.minecraftforge.items.wrapper;

import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandlerModifiable;
import legacyconnector.v1_12_2.net.minecraftforge.items.ItemHandlerHelper;

/** Forge's item handler over a vanilla inventory (the slot rules of {@code isItemValidForSlot} and the stack limits apply). */
public class InvWrapper implements IItemHandlerModifiable {
    private final IInventory inv;

    public InvWrapper(IInventory inv) {
        this.inv = inv;
    }

    public IInventory getInv() {
        return inv;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof InvWrapper w && w.inv == inv;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(inv);
    }

    @Override
    public int getSlots() {
        return inv.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack inSlot = inv.getStackInSlot(slot);
        if (!inSlot.isEmpty()) {
            if (inSlot.getCount() >= Math.min(inSlot.getMaxStackSize(), getSlotLimit(slot))) return stack;
            if (!ItemHandlerHelper.canItemStacksStack(stack, inSlot)) return stack;
            if (!inv.isItemValidForSlot(slot, stack)) return stack;
            int m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - inSlot.getCount();
            if (stack.getCount() <= m) {
                if (!simulate) {
                    ItemStack copy = stack.copy();
                    copy.grow(inSlot.getCount());
                    inv.setInventorySlotContents(slot, copy);
                    inv.markDirty();
                }
                return ItemStack.EMPTY;
            }
            stack = stack.copy();
            if (!simulate) {
                ItemStack copy = stack.splitStack(m);
                copy.grow(inSlot.getCount());
                inv.setInventorySlotContents(slot, copy);
                inv.markDirty();
            } else {
                stack.shrink(m);
            }
            return stack;
        }
        if (!inv.isItemValidForSlot(slot, stack)) return stack;
        int m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
        if (m < stack.getCount()) {
            stack = stack.copy();
            if (!simulate) {
                inv.setInventorySlotContents(slot, stack.splitStack(m));
                inv.markDirty();
            } else {
                stack.shrink(m);
            }
            return stack;
        }
        if (!simulate) {
            inv.setInventorySlotContents(slot, stack);
            inv.markDirty();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        ItemStack inSlot = inv.getStackInSlot(slot);
        if (inSlot.isEmpty()) return ItemStack.EMPTY;
        if (simulate) {
            if (inSlot.getCount() < amount) return inSlot.copy();
            ItemStack copy = inSlot.copy();
            copy.setCount(amount);
            return copy;
        }
        int m = Math.min(inSlot.getCount(), amount);
        ItemStack decr = inv.decrStackSize(slot, m);
        inv.markDirty();
        return decr;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        inv.setInventorySlotContents(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inv.getInventoryStackLimit();
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return inv.isItemValidForSlot(slot, stack);
    }
}
