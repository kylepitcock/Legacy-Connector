package legacyconnector.v1_7_10.net.minecraft.village;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A villager trade: one or two stacks bought for one sold, with a use counter. */
public class MerchantRecipe {
    private ItemStack itemToBuy;
    private ItemStack secondItemToBuy;
    private ItemStack itemToSell;
    private int toolUses;
    private int maxTradeUses;

    public MerchantRecipe(ItemStack buy, ItemStack sell) {
        this(buy, null, sell);
    }

    public MerchantRecipe(ItemStack buy, legacyconnector.v1_7_10.net.minecraft.item.Item sell) {
        this(buy, new ItemStack(sell));
    }

    public MerchantRecipe(ItemStack buy, legacyconnector.v1_7_10.net.minecraft.block.Block sell) {
        this(buy, new ItemStack(sell));
    }

    public MerchantRecipe(ItemStack buy, ItemStack secondBuy, ItemStack sell) {
        this.itemToBuy = buy;
        this.secondItemToBuy = secondBuy;
        this.itemToSell = sell;
        this.maxTradeUses = 7;
    }

    public ItemStack getItemToBuy() {
        return itemToBuy;
    }

    public ItemStack getSecondItemToBuy() {
        return secondItemToBuy;
    }

    public boolean hasSecondItemToBuy() {
        return secondItemToBuy != null;
    }

    public ItemStack getItemToSell() {
        return itemToSell;
    }

    public int getToolUses() {
        return toolUses;
    }

    public int getMaxTradeUses() {
        return maxTradeUses;
    }

    public void incrementToolUses() {
        toolUses++;
    }

    public void func_82783_a(int amount) {
        increaseMaxTradeUses(amount);
    }

    public void increaseMaxTradeUses(int amount) {
        maxTradeUses += amount;
    }

    public boolean isRecipeDisabled() {
        return toolUses >= maxTradeUses;
    }

    public void func_82785_h() {
        toolUses = maxTradeUses;
    }
}
