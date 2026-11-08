package legacyconnector.v1_7_10.codechicken.lib.inventory;

import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** CodeChickenLib's item stack and inventory helpers. */
public class InventoryUtils {
    /** The stack's real damage (a negative value of an item without subtypes counts as 0). */
    public static int actualDamage(ItemStack stack) {
        return stack.getHasSubtypes() || stack.getMaxStackSize() > 0 ? Math.max(stack.getItemDamage(), 0) : 0;
    }

    public static ItemStack copyStack(ItemStack stack, int quantity) {
        if (stack == null) return null;
        stack = stack.copy();
        ItemStack.mcc$set$stackSize(stack, quantity);
        return stack;
    }

    public static ItemStack newItemStack(legacyconnector.v1_7_10.net.minecraft.item.Item item, int quantity, int damage) {
        return new ItemStack(item, quantity, damage);
    }

    /** Same item, damage and tag. */
    public static boolean areStacksIdentical(ItemStack a, ItemStack b) {
        if (a == null || b == null) return a == b;
        return a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** Whether the two stacks could be merged into one (same item, damage and tag; either may be null). */
    public static boolean canStack(ItemStack a, ItemStack b) {
        return a == null || b == null || (a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b) && a.isStackable());
    }

    public static int incrStackSize(ItemStack base, int addition) {
        if (base == null) return 0;
        ItemStack.mcc$set$stackSize(base, ItemStack.mcc$get$stackSize(base) + addition);
        return ItemStack.mcc$get$stackSize(base);
    }

    public static int getInsertibleQuantity(IInventory inv, ItemStack stack) {
        int space = 0;
        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            space += slotInsertibleQuantity(inv, slot, stack);
        }
        return space;
    }

    private static int slotInsertibleQuantity(IInventory inv, int slot, ItemStack stack) {
        if (!inv.isItemValidForSlot(slot, stack)) return 0;
        ItemStack in = inv.getStackInSlot(slot);
        int limit = Math.min(inv.getInventoryStackLimit(), stack.getMaxStackSize());
        if (in == null) return limit;
        if (!canStack(in, stack)) return 0;
        return Math.max(0, limit - ItemStack.mcc$get$stackSize(in));
    }

    /** Inserts as much of the stack as fits into the inventory; returns how many items went in (simulate leaves it unchanged). */
    public static int insertItem(IInventory inv, ItemStack stack, boolean simulate) {
        int remaining = ItemStack.mcc$get$stackSize(stack);
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = 0; slot < inv.getSizeInventory() && remaining > 0; slot++) {
                ItemStack in = inv.getStackInSlot(slot);
                if ((pass == 0) != (in != null)) continue;
                int fit = Math.min(remaining, slotInsertibleQuantity(inv, slot, stack));
                if (fit <= 0) continue;
                if (!simulate) {
                    if (in == null) inv.setInventorySlotContents(slot, copyStack(stack, fit));
                    else incrStackSize(in, fit);
                }
                remaining -= fit;
            }
        }
        if (!simulate) inv.markDirty();
        return ItemStack.mcc$get$stackSize(stack) - remaining;
    }

    /** Inserts through the given side of a sided inventory (only slots it exposes for insertion). */
    public static int insertItem(IInventory inv, ItemStack stack, boolean simulate, int side) {
        if (!(inv instanceof ISidedInventory sided)) return insertItem(inv, stack, simulate);
        int remaining = ItemStack.mcc$get$stackSize(stack);
        for (int slot : sided.getAccessibleSlotsFromSide(side)) {
            if (remaining <= 0) break;
            if (!sided.canInsertItem(slot, stack, side)) continue;
            int fit = Math.min(remaining, slotInsertibleQuantity(inv, slot, stack));
            if (fit <= 0) continue;
            if (!simulate) {
                ItemStack in = inv.getStackInSlot(slot);
                if (in == null) inv.setInventorySlotContents(slot, copyStack(stack, fit));
                else incrStackSize(in, fit);
            }
            remaining -= fit;
        }
        if (!simulate) inv.markDirty();
        return ItemStack.mcc$get$stackSize(stack) - remaining;
    }

    /** The first stack of the inventory that could be taken out (null if empty). */
    public static ItemStack getExtractableStack(IInventory inv, int slot) {
        ItemStack stack = inv.getStackInSlot(slot);
        return stack == null ? null : stack.copy();
    }

    /** Takes up to {@code amount} items out of the slot; returns what was taken. */
    public static ItemStack decrStackSize(IInventory inv, int slot, int amount) {
        return inv.decrStackSize(slot, amount);
    }

    public static void dropItem(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.world.World world, double x, double y, double z) {
        if (stack == null || ItemStack.mcc$get$stackSize(stack) <= 0) return;
        world.spawnEntityInWorld(new legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem(world, x, y, z, stack));
    }
}
