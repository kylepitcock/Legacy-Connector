package legacyconnector.v1_12_2.net.minecraft.village;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** 1.12.2 villager trade: one or two stacks to pay, a stack to get, uses and a use limit (verbatim 1.12 logic). */
public class MerchantRecipe {
    private ItemStack itemToBuy = ItemStack.EMPTY;
    private ItemStack secondItemToBuy = ItemStack.EMPTY;
    private ItemStack itemToSell = ItemStack.EMPTY;
    private int toolUses;
    private int maxTradeUses;
    private boolean rewardsExp;

    public MerchantRecipe(NBTTagCompound tag) {
        readFromTags(tag);
    }

    public MerchantRecipe(ItemStack buy1, ItemStack buy2, ItemStack sell) {
        this(buy1, buy2, sell, 0, 7);
    }

    public MerchantRecipe(ItemStack buy1, ItemStack buy2, ItemStack sell, int toolUses, int maxTradeUses) {
        this.itemToBuy = buy1 == null ? ItemStack.EMPTY : buy1;
        this.secondItemToBuy = buy2 == null ? ItemStack.EMPTY : buy2;
        this.itemToSell = sell == null ? ItemStack.EMPTY : sell;
        this.toolUses = toolUses;
        this.maxTradeUses = maxTradeUses;
        this.rewardsExp = true;
    }

    public MerchantRecipe(ItemStack buy1, ItemStack sell) {
        this(buy1, ItemStack.EMPTY, sell);
    }

    public MerchantRecipe(ItemStack buy1, Item sell) {
        this(buy1, new ItemStack(sell));
    }

    public ItemStack getItemToBuy() {
        return itemToBuy;
    }

    public ItemStack getSecondItemToBuy() {
        return secondItemToBuy;
    }

    public boolean hasSecondItemToBuy() {
        return !secondItemToBuy.isEmpty();
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
        ++toolUses;
    }

    public void increaseMaxTradeUses(int amount) {
        maxTradeUses += amount;
    }

    public boolean isRecipeDisabled() {
        return toolUses >= maxTradeUses;
    }

    public void compensateToolUses() {
        toolUses = maxTradeUses;
    }

    public boolean getRewardsExp() {
        return rewardsExp;
    }

    public void readFromTags(NBTTagCompound tag) {
        itemToBuy = new ItemStack(tag.getCompoundTag("buy"));
        itemToSell = new ItemStack(tag.getCompoundTag("sell"));
        secondItemToBuy = tag.hasKey("buyB", 10) ? new ItemStack(tag.getCompoundTag("buyB")) : ItemStack.EMPTY;
        toolUses = tag.hasKey("uses", 99) ? tag.getInteger("uses") : 0;
        maxTradeUses = tag.hasKey("maxUses", 99) ? tag.getInteger("maxUses") : 7;
        rewardsExp = !tag.hasKey("rewardExp", 1) || tag.getBoolean("rewardExp");
    }

    public NBTTagCompound writeToTags() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("buy", itemToBuy.writeToNBT(new NBTTagCompound()));
        tag.setTag("sell", itemToSell.writeToNBT(new NBTTagCompound()));
        if (!secondItemToBuy.isEmpty()) tag.setTag("buyB", secondItemToBuy.writeToNBT(new NBTTagCompound()));
        tag.setInteger("uses", toolUses);
        tag.setInteger("maxUses", maxTradeUses);
        tag.setBoolean("rewardExp", rewardsExp);
        return tag;
    }

    // ------------------------------------------------------------------ the modern trade

    /** The modern offer for this trade (payment matched by item and, when the stack has a tag, by its components). */
    public net.minecraft.world.item.trading.MerchantOffer mcc$toModern() {
        var costA = mcc$cost(itemToBuy);
        if (costA == null) return null;
        var costB = java.util.Optional.ofNullable(mcc$cost(secondItemToBuy));
        var result = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(itemToSell);
        if (result.isEmpty()) return null;
        // villager experience 1 per trade, as 1.12's trades levelled careers by use count; no xp when the trade gives none
        return new net.minecraft.world.item.trading.MerchantOffer(costA, costB, result, toolUses, maxTradeUses, rewardsExp ? 1 : 0, 0.05F);
    }

    private static net.minecraft.world.item.trading.ItemCost mcc$cost(ItemStack legacy) {
        if (legacy == null || legacy.isEmpty()) return null;
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(legacy);
        if (modern.isEmpty()) return null;
        var predicate = net.minecraft.core.component.DataComponentPredicate.EMPTY;
        if (!modern.getComponentsPatch().isEmpty()) {
            var builder = net.minecraft.core.component.DataComponentPredicate.builder();
            for (var entry : modern.getComponentsPatch().entrySet()) {
                entry.getValue().ifPresent(v -> mcc$expect(builder, entry.getKey(), v));
            }
            predicate = builder.build();
        }
        return new net.minecraft.world.item.trading.ItemCost(modern.getItemHolder(), modern.getCount(), predicate);
    }

    @SuppressWarnings("unchecked")
    private static <T> void mcc$expect(net.minecraft.core.component.DataComponentPredicate.Builder builder,
            net.minecraft.core.component.DataComponentType<T> type, Object value) {
        builder.expect(type, (T) value);
    }

    /** A legacy trade for a modern offer. */
    public static MerchantRecipe mcc$of(net.minecraft.world.item.trading.MerchantOffer offer) {
        var fromModern = (java.util.function.Function<net.minecraft.world.item.ItemStack, ItemStack>) org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122::fromModern;
        MerchantRecipe r = new MerchantRecipe(fromModern.apply(offer.getBaseCostA()), fromModern.apply(offer.getCostB()), fromModern.apply(offer.getResult()),
                offer.getUses(), offer.getMaxUses());
        r.rewardsExp = offer.shouldRewardExp();
        return r;
    }
}
