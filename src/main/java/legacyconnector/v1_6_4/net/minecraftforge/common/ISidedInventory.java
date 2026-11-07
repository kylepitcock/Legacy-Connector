package legacyconnector.v1_6_4.net.minecraftforge.common;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * Forge 1.5.2's {@code net.minecraftforge.common.ISidedInventory}: the slots reachable from a side are a start index and a size
 * ({@code getStartInventorySide}, {@code getSizeInventorySide}). 1.7.10's interface lists the slots instead; this one is that interface with
 * the legacy pair as the abstract methods and the 1.7.10 ones derived from them, so a 1.5.2 inventory is a 1.7.10 sided inventory for
 * everything that asks ({@code TileEntityHopper}, the capability adapters, ...).
 */
public interface ISidedInventory extends legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory {
    int getStartInventorySide(ForgeDirection side);

    int getSizeInventorySide(ForgeDirection side);

    @Override
    default int[] getAccessibleSlotsFromSide(int side) {
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        int start = getStartInventorySide(direction);
        int size = Math.max(0, getSizeInventorySide(direction));
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) slots[i] = start + i;
        return slots;
    }

    @Override
    default boolean canInsertItem(int slot, ItemStack stack, int side) {
        return inSide(slot, side);
    }

    @Override
    default boolean canExtractItem(int slot, ItemStack stack, int side) {
        return inSide(slot, side);
    }

    private boolean inSide(int slot, int side) {
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        int start = getStartInventorySide(direction);
        return slot >= start && slot < start + getSizeInventorySide(direction);
    }
}
