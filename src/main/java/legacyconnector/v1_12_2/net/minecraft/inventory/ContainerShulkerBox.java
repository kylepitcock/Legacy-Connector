package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The shulker box GUI's container: its 27 slots (no shulker boxes inside) above the player's inventory. */
public class ContainerShulkerBox extends Container {
    private final IInventory inventory;

    public ContainerShulkerBox(IInventory playerInventoryIn, IInventory inventoryIn, EntityPlayer player) {
        this.inventory = inventoryIn;
        inventoryIn.openInventory(player);
        IInventory playerInventory = playerInventoryIn;
        for (int j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) addSlotToContainer(new SlotShulkerBox(inventoryIn, k + j * 9, 8 + k * 18, 18 + j * 18));
        }
        for (int l = 0; l < 3; ++l) {
            for (int k = 0; k < 9; ++k) addSlotToContainer(new Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, 84 + l * 18));
        }
        for (int i1 = 0; i1 < 9; ++i1) addSlotToContainer(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return inventory.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            int own = inventory.getSizeInventory();
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
        inventory.closeInventory(playerIn);
    }
}
