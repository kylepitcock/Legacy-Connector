package legacyconnector.v1_12_2.baubles.common.container;

import legacyconnector.v1_12_2.baubles.api.cap.IBaublesItemHandler;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.items.SlotItemHandler;

/** Baubles: a container slot over one slot of the player's Baubles handler (an addon's own screen, e.g. a bauble box, reuses it). */
public class SlotBauble extends SlotItemHandler {
    private final int baubleSlot;
    private final EntityPlayer player;

    public SlotBauble(EntityPlayer player, IBaublesItemHandler itemHandler, int slot, int x, int y) {
        super(itemHandler, slot, x, y);
        this.baubleSlot = slot;
        this.player = player;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return ((IBaublesItemHandler) getItemHandler()).isItemValidForSlot(baubleSlot, stack, player);
    }

    @Override
    public boolean canTakeStack(EntityPlayer playerIn) {
        ItemStack stack = getStack();
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof legacyconnector.v1_12_2.baubles.api.IBauble b) return b.canUnequip(stack, playerIn);
        return true;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}
