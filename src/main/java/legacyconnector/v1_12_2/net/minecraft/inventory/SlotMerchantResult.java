package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.IMerchant;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipe;

/** The result slot of a trade: taking from it pays with the payment slots and tells the merchant the recipe was used. */
public class SlotMerchantResult extends Slot {
    private final InventoryMerchant merchantInventory;
    private final EntityPlayer player;
    private int removeCount;
    private final IMerchant merchant;

    public SlotMerchantResult(EntityPlayer player, IMerchant merchant, InventoryMerchant merchantInventory, int slotIndex, int xPosition, int yPosition) {
        super(merchantInventory, slotIndex, xPosition, yPosition);
        this.player = player;
        this.merchant = merchant;
        this.merchantInventory = merchantInventory;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        if (getHasStack()) removeCount += Math.min(amount, getStack().getCount());
        return super.decrStackSize(amount);
    }

    @Override
    protected void onCrafting(ItemStack stack, int amount) {
        removeCount += amount;
        onCrafting(stack);
    }

    @Override
    protected void onCrafting(ItemStack stack) {
        stack.onCrafting(player.getEntityWorld(), player, removeCount);
        removeCount = 0;
    }

    @Override
    public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack) {
        onCrafting(stack);
        MerchantRecipe recipe = merchantInventory.getCurrentRecipe();
        if (recipe != null) {
            ItemStack first = merchantInventory.getStackInSlot(0);
            ItemStack second = merchantInventory.getStackInSlot(1);
            if (doTrade(recipe, first, second) || doTrade(recipe, second, first)) {
                merchant.useRecipe(recipe);
                thePlayer.addStat(legacyconnector.v1_12_2.net.minecraft.stats.StatList.TRADED_WITH_VILLAGER);
                merchantInventory.setInventorySlotContents(0, first);
                merchantInventory.setInventorySlotContents(1, second);
            }
        }
        return stack;
    }

    private boolean doTrade(MerchantRecipe trade, ItemStack firstItem, ItemStack secondItem) {
        ItemStack buy1 = trade.getItemToBuy();
        ItemStack buy2 = trade.getSecondItemToBuy();
        if (firstItem.getItem() == buy1.getItem() && firstItem.getCount() >= buy1.getCount()) {
            if (!buy2.isEmpty() && !secondItem.isEmpty() && buy2.getItem() == secondItem.getItem() && secondItem.getCount() >= buy2.getCount()) {
                firstItem.shrink(buy1.getCount());
                secondItem.shrink(buy2.getCount());
                return true;
            }
            if (buy2.isEmpty() && secondItem.isEmpty()) {
                firstItem.shrink(buy1.getCount());
                return true;
            }
        }
        return false;
    }
}
