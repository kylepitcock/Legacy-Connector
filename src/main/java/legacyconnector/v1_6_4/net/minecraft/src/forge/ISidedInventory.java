package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** Forge 3.x's sided inventory: the slots reachable from a side are a start index and a size. The shim's sided inventory is derived from them. */
public interface ISidedInventory extends legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory {
    int getStartInventorySide(int side);

    int getSizeInventorySide(int side);

    @Override
    default int[] getAccessibleSlotsFromSide(int side) {
        int start = getStartInventorySide(side);
        int size = Math.max(0, getSizeInventorySide(side));
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) slots[i] = start + i;
        return slots;
    }

    @Override
    default boolean canInsertItem(int slot, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, int side) {
        int start = getStartInventorySide(side);
        return slot >= start && slot < start + getSizeInventorySide(side);
    }

    @Override
    default boolean canExtractItem(int slot, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, int side) {
        return canInsertItem(slot, stack, side);
    }
}
