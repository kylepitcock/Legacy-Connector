package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The chest GUI's container: the chest's rows of nine above the player's inventory. */
public class ContainerChest extends Container {
    private final IInventory lowerChestInventory;
    private final int numRows;

    public ContainerChest(IInventory playerInventory, IInventory chestInventory, EntityPlayer player) {
        this.lowerChestInventory = chestInventory;
        this.numRows = chestInventory.getSizeInventory() / 9;
        chestInventory.openInventory(player);
        int i = (numRows - 4) * 18;
        for (int j = 0; j < numRows; ++j) {
            for (int k = 0; k < 9; ++k) addSlotToContainer(new Slot(chestInventory, k + j * 9, 8 + k * 18, 18 + j * 18));
        }
        for (int l = 0; l < 3; ++l) {
            for (int j1 = 0; j1 < 9; ++j1) addSlotToContainer(new Slot(playerInventory, j1 + l * 9 + 9, 8 + j1 * 18, 103 + l * 18 + i));
        }
        for (int i1 = 0; i1 < 9; ++i1) addSlotToContainer(new Slot(playerInventory, i1, 8 + i1 * 18, 161 + i));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return lowerChestInventory.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < numRows * 9) {
                if (!mergeItemStack(itemstack1, numRows * 9, inventorySlots.size(), true)) return ItemStack.EMPTY;
            } else if (!mergeItemStack(itemstack1, 0, numRows * 9, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
        }
        return itemstack;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        lowerChestInventory.closeInventory(playerIn);
    }

    public IInventory getLowerChestInventory() {
        return lowerChestInventory;
    }
}
