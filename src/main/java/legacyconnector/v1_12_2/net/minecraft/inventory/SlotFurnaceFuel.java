package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The fuel slot of a furnace: fuels and empty buckets (one at a time). */
public class SlotFurnaceFuel extends Slot {
    public SlotFurnaceFuel(IInventory inventoryIn, int slotIndex, int xPosition, int yPosition) {
        super(inventoryIn, slotIndex, xPosition, yPosition);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.SynthItems1122.burnTime(stack) > 0 || isBucket(stack);
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return isBucket(stack) ? 1 : super.getItemStackLimit(stack);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.BUCKET;
    }
}
