package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** An inventory whose slots can be reached from only some sides (side: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east). */
public interface ISidedInventory extends IInventory {
    int[] getAccessibleSlotsFromSide(int side);

    boolean canInsertItem(int slot, ItemStack stack, int side);

    boolean canExtractItem(int slot, ItemStack stack, int side);
}
