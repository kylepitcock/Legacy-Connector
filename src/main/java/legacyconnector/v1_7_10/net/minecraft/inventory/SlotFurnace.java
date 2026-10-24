package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A furnace output slot: nothing can be put in, the result can be taken. */
public class SlotFurnace extends Slot {
    private final EntityPlayer thePlayer;

    public SlotFurnace(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.thePlayer = player;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }
}
