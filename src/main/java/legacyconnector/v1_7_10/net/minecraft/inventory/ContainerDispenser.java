package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityDispenser;

/** The menu of a dispenser or dropper (1.7.10 {@code ContainerDispenser}): its 3x3 slots, then the player inventory. */
public class ContainerDispenser extends Container {
    private final TileEntityDispenser tileEntityDispenser;

    public ContainerDispenser(IInventory playerInventory, TileEntityDispenser dispenser) {
        this.tileEntityDispenser = dispenser;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) addSlotToContainer(new Slot(dispenser, col + row * 3, 62 + col * 18, 17 + row * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tileEntityDispenser.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < 9) {
                if (!mergeItemStack(stack, 9, 45, true)) return null;
            } else if (!mergeItemStack(stack, 0, 9, false)) {
                return null;
            }
            if (stack.mcc$count() == 0) slot.putStack(null);
            else slot.onSlotChanged();
            if (stack.mcc$count() == result.mcc$count()) return null;
            slot.onPickupFromSlot(player, stack);
        }
        return result;
    }
}
