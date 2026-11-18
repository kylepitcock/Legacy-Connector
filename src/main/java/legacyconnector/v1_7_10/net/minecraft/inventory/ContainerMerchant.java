package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.IMerchant;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The trading menu (1.7.10 {@code ContainerMerchant}): two payment slots, the result, then the player inventory; closing returns the payment. */
public class ContainerMerchant extends Container {
    private final IMerchant theMerchant;
    private final InventoryMerchant merchantInventory;
    private final World theWorld;

    public ContainerMerchant(InventoryPlayer playerInventory, IMerchant merchant, World world) {
        this.theMerchant = merchant;
        this.theWorld = world;
        this.merchantInventory = new InventoryMerchant(playerInventory.player, merchant);
        addSlotToContainer(new Slot(merchantInventory, 0, 36, 53));
        addSlotToContainer(new Slot(merchantInventory, 1, 62, 53));
        addSlotToContainer(new SlotMerchantResult(playerInventory.player, merchant, merchantInventory, 2, 120, 53));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    public InventoryMerchant getMerchantInventory() {
        return merchantInventory;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        merchantInventory.resetRecipeAndSlots();
        super.onCraftMatrixChanged(inventory);
    }

    /** Selects which of the merchant's trades the payment slots are matched against (the arrows of the trading screen). */
    public void setCurrentRecipeIndex(int index) {
        merchantInventory.setCurrentRecipeIndex(index);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return theMerchant.getCustomer() == player;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index == 2) {
                if (!mergeItemStack(stack, 3, 39, true)) return null;
                slot.onSlotChange(stack, result);
            } else if (index != 0 && index != 1) {
                if (index >= 3 && index < 30) {
                    if (!mergeItemStack(stack, 30, 39, false)) return null;
                } else if (index >= 30 && index < 39 && !mergeItemStack(stack, 3, 30, false)) {
                    return null;
                }
            } else if (!mergeItemStack(stack, 3, 39, false)) {
                return null;
            }
            if (stack.mcc$count() == 0) slot.putStack(null);
            else slot.onSlotChanged();
            if (stack.mcc$count() == result.mcc$count()) return null;
            slot.onPickupFromSlot(player, stack);
        }
        return result;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        theMerchant.setCustomer(null);
        if (!theWorld.isRemote) {
            ItemStack first = merchantInventory.getStackInSlotOnClosing(0);
            if (first != null) player.dropPlayerItemWithRandomChoice(first, false);
            ItemStack second = merchantInventory.getStackInSlotOnClosing(1);
            if (second != null) player.dropPlayerItemWithRandomChoice(second, false);
        }
    }
}
