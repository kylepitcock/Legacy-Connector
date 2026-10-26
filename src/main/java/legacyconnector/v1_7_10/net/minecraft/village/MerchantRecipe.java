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

    public MerchantRecipe(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        readFromTags(tag);
    }

    /** Reads the 1.7.10 trade format: {@code buy}, optional {@code buyB}, {@code sell}, {@code uses}, {@code maxUses} (7 when absent). */
    public void readFromTags(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        itemToBuy = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("buy"));
        itemToSell = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("sell"));
        secondItemToBuy = tag.hasKey("buyB", 10) ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("buyB")) : null;
        if (tag.hasKey("uses", 99)) toolUses = tag.getInteger("uses");
        maxTradeUses = tag.hasKey("maxUses", 99) ? tag.getInteger("maxUses") : 7;
    }

    public legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound writeToTags() {
        var tag = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
        if (itemToBuy != null) tag.setTag("buy", itemToBuy.writeToNBT(new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound()));
        if (itemToSell != null) tag.setTag("sell", itemToSell.writeToNBT(new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound()));
        if (secondItemToBuy != null) tag.setTag("buyB", secondItemToBuy.writeToNBT(new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound()));
        tag.setInteger("uses", toolUses);
        tag.setInteger("maxUses", maxTradeUses);
        return tag;
    }

    /** Whether the other trade buys and sells the same items (ignoring counts): the same offer, maybe with other amounts. */
    public boolean hasSameIDsAs(MerchantRecipe other) {
        if (other == null || itemToBuy == null || itemToSell == null || other.itemToBuy == null || other.itemToSell == null) return false;
        if (itemToBuy.getItem() != other.itemToBuy.getItem() || itemToSell.getItem() != other.itemToSell.getItem()) return false;
        if (secondItemToBuy == null) return other.secondItemToBuy == null;
        return other.secondItemToBuy != null && secondItemToBuy.getItem() == other.secondItemToBuy.getItem();
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
