package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** An inventory that exposes different slots on each side (furnaces: input on top, fuel on the sides, output below). */
public interface ISidedInventory extends IInventory {
    int[] getSlotsForFace(EnumFacing side);

    boolean canInsertItem(int index, ItemStack stack, EnumFacing direction);

    boolean canExtractItem(int index, ItemStack stack, EnumFacing direction);
}
