package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** A shulker box slot: anything but another shulker box. */
public class SlotShulkerBox extends Slot {
    public SlotShulkerBox(IInventory p_i47265_1_, int slotIndexIn, int xPosition, int yPosition) {
        super(p_i47265_1_, slotIndexIn, xPosition, yPosition);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
        return !(modern.getItem() instanceof net.minecraft.world.item.BlockItem b && b.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock);
    }
}
