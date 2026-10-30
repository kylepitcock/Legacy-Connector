package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.IMerchant;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 trading container: two payment slots, the trade's result and the player's inventory (mods' traders reuse it). */
public class ContainerMerchant extends Container {
    private IMerchant merchant;
    private final InventoryMerchant merchantInventory;
    private final World world;

    public ContainerMerchant(InventoryPlayer playerInventory, IMerchant merchant, World worldIn) {
        this.merchant = merchant;
        this.world = worldIn;
        this.merchantInventory = new InventoryMerchant(playerInventory.player, merchant);
        addSlotToContainer(new Slot(merchantInventory, 0, 36, 53));
        addSlotToContainer(new Slot(merchantInventory, 1, 62, 53));
        addSlotToContainer(new SlotMerchantResult(playerInventory.player, merchant, merchantInventory, 2, 120, 53));
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        }
        for (int k = 0; k < 9; ++k) addSlotToContainer(new Slot(playerInventory, k, 8 + k * 18, 142));
    }

    public InventoryMerchant getMerchantInventory() {
        return merchantInventory;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        merchantInventory.resetRecipeAndSlots();
        super.onCraftMatrixChanged(inventoryIn);
    }

    /** The trade the player selected in the screen. */
    public void setCurrentRecipeIndex(int currentRecipeIndex) {
        merchantInventory.setCurrentRecipeIndex(currentRecipeIndex);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return merchant.getCustomer() == playerIn;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index == 2) {
                if (!mergeItemStack(itemstack1, 3, 39, true)) return ItemStack.EMPTY;
                slot.onSlotChange(itemstack1, itemstack);
            } else if (index != 0 && index != 1) {
                if (index >= 3 && index < 30) {
                    if (!mergeItemStack(itemstack1, 30, 39, false)) return ItemStack.EMPTY;
                } else if (index >= 30 && index < 39 && !mergeItemStack(itemstack1, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    /** Closing ends the trade and gives the payment back. */
    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        merchant.setCustomer(null);
        if (world != null && !world.isRemote && playerIn != null) {
            ItemStack stack = merchantInventory.removeStackFromSlot(0);
            if (!stack.isEmpty()) playerIn.dropItem(stack, false);
            stack = merchantInventory.removeStackFromSlot(1);
            if (!stack.isEmpty()) playerIn.dropItem(stack, false);
        }
    }
}
