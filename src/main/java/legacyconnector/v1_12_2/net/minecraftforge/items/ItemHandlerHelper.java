package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

public class ItemHandlerHelper {
    public static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty()) return stack;
        for (int i = 0; i < dest.getSlots(); i++) {
            stack = dest.insertItem(i, stack, simulate);
            if (stack.isEmpty()) return ItemStack.EMPTY;
        }
        return stack;
    }

    public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
        if (a.isEmpty() || !a.isItemEqual(b) || a.hasTagCompound() != b.hasTagCompound()) return false;
        return !a.hasTagCompound() || a.getTagCompound().equals(b.getTagCompound());
    }

    public static ItemStack copyStackWithSize(ItemStack stack, int size) {
        if (size == 0) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(size);
        return copy;
    }
}
