package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The hopper GUI's container: the hopper's five slots above the player's inventory. */
public class ContainerHopper extends Container {
    private final IInventory hopperInventory;

    public ContainerHopper(legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer playerInventory, IInventory hopperInventory, EntityPlayer player) {
        this.hopperInventory = hopperInventory;
        hopperInventory.openInventory(player);
        for (int j = 0; j < hopperInventory.getSizeInventory(); ++j) addSlotToContainer(new Slot(hopperInventory, j, 44 + j * 18, 20));
        for (int l = 0; l < 3; ++l) {
            for (int k = 0; k < 9; ++k) addSlotToContainer(new Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, 51 + l * 18));
        }
        for (int i1 = 0; i1 < 9; ++i1) addSlotToContainer(new Slot(playerInventory, i1, 8 + i1 * 18, 109));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return hopperInventory.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            int own = hopperInventory.getSizeInventory();
            if (index < own) {
                if (!mergeItemStack(itemstack1, own, inventorySlots.size(), true)) return ItemStack.EMPTY;
            } else if (!mergeItemStack(itemstack1, 0, own, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        hopperInventory.closeInventory(playerIn);
    }
}
