package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** The menu of a chest-like inventory: its rows on top, the player inventory below. */
public class ContainerChest extends Container {
    private final IInventory lowerChestInventory;
    private final int numRows;

    public ContainerChest(IInventory playerInventory, IInventory chestInventory) {
        this.lowerChestInventory = chestInventory;
        this.numRows = chestInventory.getSizeInventory() / 9;
        chestInventory.openInventory();
        int yOffset = (numRows - 4) * 18;
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(chestInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + yOffset));
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 161 + yOffset));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return lowerChestInventory.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < numRows * 9) {
                if (!mergeItemStack(stack, numRows * 9, inventorySlots.size(), true)) return null;
            } else if (!mergeItemStack(stack, 0, numRows * 9, false)) {
                return null;
            }
            if (stack.mcc$count() == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }
        return result;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        lowerChestInventory.closeInventory();
    }

    public IInventory getLowerChestInventory() {
        return lowerChestInventory;
    }
}
