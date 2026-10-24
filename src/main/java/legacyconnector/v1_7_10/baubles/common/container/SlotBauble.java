package legacyconnector.v1_7_10.baubles.common.container;

import legacyconnector.v1_7_10.baubles.api.IBauble;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.inventory.Slot;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A slot of the Baubles inventory screen: holds only baubles that may be equipped. */
public class SlotBauble extends Slot {
    private final int baubleSlot;

    public SlotBauble(IInventory inventory, int baubleSlot, int x, int y) {
        super(inventory, baubleSlot, x, y);
        this.baubleSlot = baubleSlot;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IBauble;
    }

    public int mcc$baubleSlot() {
        return baubleSlot;
    }
}
