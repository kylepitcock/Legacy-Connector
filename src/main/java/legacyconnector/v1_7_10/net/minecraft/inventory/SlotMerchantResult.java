package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.IMerchant;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipe;

/** The result slot of a trade (1.7.10 {@code SlotMerchantResult}): taking the wares uses up the payment and the trade's counter. */
public class SlotMerchantResult extends Slot {
    private final InventoryMerchant theMerchantInventory;
    private final EntityPlayer thePlayer;
    private int field_75231_g;
    private final IMerchant theMerchant;

    public SlotMerchantResult(EntityPlayer player, IMerchant merchant, InventoryMerchant inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.thePlayer = player;
        this.theMerchant = merchant;
        this.theMerchantInventory = inventory;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        if (getHasStack()) field_75231_g += Math.min(amount, getStack().mcc$count());
        return super.decrStackSize(amount);
    }

    @Override
    protected void onCrafting(ItemStack stack, int amount) {
        field_75231_g += amount;
        onCrafting(stack);
    }

    @Override
    protected void onCrafting(ItemStack stack) {
        stack.onCrafting(Entity.mcc$get$worldObj(thePlayer), thePlayer, field_75231_g);
        field_75231_g = 0;
    }

    @Override
    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
        onCrafting(stack);
        MerchantRecipe recipe = theMerchantInventory.getCurrentRecipe();
        if (recipe == null) return;
        ItemStack first = inventory.getStackInSlot(0);
        ItemStack second = inventory.getStackInSlot(1);
        if (func_75230_a(recipe, first, second) || func_75230_a(recipe, second, first)) {
            theMerchant.useRecipe(recipe);
            if (first != null && first.mcc$count() <= 0) first = null;
            if (second != null && second.mcc$count() <= 0) second = null;
            inventory.setInventorySlotContents(0, first);
            inventory.setInventorySlotContents(1, second);
        }
    }

    /** Takes the trade's price out of {@code first} (and {@code second}); false when they do not match the trade. */
    private boolean func_75230_a(MerchantRecipe recipe, ItemStack first, ItemStack second) {
        ItemStack buy = recipe.getItemToBuy();
        ItemStack buySecond = recipe.getSecondItemToBuy();
        if (first != null && first.getItem() == buy.getItem()) {
            if (buySecond != null && second != null && buySecond.getItem() == second.getItem()) {
                ItemStack.mcc$set$stackSize(first, first.mcc$count() - buy.mcc$count());
                ItemStack.mcc$set$stackSize(second, second.mcc$count() - buySecond.mcc$count());
                return true;
            }
            if (buySecond == null && second == null) {
                ItemStack.mcc$set$stackSize(first, first.mcc$count() - buy.mcc$count());
                return true;
            }
        }
        return false;
    }
}
