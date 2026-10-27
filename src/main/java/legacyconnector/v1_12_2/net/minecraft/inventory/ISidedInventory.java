package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** An inventory whose slots depend on the face it is accessed from (furnaces, machines). */
public interface ISidedInventory extends IInventory {
    int[] getSlotsForFace(EnumFacing side);

    boolean canInsertItem(int index, ItemStack stack, EnumFacing direction);

    boolean canExtractItem(int index, ItemStack stack, EnumFacing direction);
}
