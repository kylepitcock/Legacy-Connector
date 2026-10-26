package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Optional;
import legacyconnector.v1_7_10.net.minecraft.entity.IMerchant;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipe;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** A legacy {@link IMerchant} traded with through the modern trading screen: its recipes become offers, trades call useRecipe. */
public final class MerchantBridge {
    private MerchantBridge() {}

    /** Opens the trading screen of {@code merchant} for {@code player} (server side; the client follows the server). */
    public static void open(EntityPlayer player, IMerchant merchant, String name) {
        if (player == null || merchant == null || !(player.mcc$entity instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        Merchant modern = merchant instanceof legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager v
                && v.mcc$entity instanceof net.minecraft.world.entity.npc.Villager villager && !v.mcc$hosted() ? villager : new Adapter(merchant);
        modern.setTradingPlayer(sp);
        Component title = name == null || name.isEmpty() ? Component.translatable("entity.minecraft.villager") : Component.literal(name);
        modern.openTradingScreen(sp, title, 1);
    }

    static MerchantOffer offer(MerchantRecipe recipe) {
        var buy = ItemStack.mcc$toModern(recipe.getItemToBuy());
        var sell = ItemStack.mcc$toModern(recipe.getItemToSell());
        if (buy == null || sell == null || buy.isEmpty() || sell.isEmpty()) return null;
        Optional<ItemCost> second = Optional.empty();
        if (recipe.hasSecondItemToBuy()) {
            var s = ItemStack.mcc$toModern(recipe.getSecondItemToBuy());
            if (s != null && !s.isEmpty()) second = Optional.of(new ItemCost(s.getItem(), s.getCount()));
        }
        return new MerchantOffer(new ItemCost(buy.getItem(), buy.getCount()), second, sell, recipe.getToolUses(), Math.max(1, recipe.getMaxTradeUses()), 0, 0.0F);
    }

    /** The modern merchant side of a legacy one. */
    static final class Adapter implements Merchant {
        private final IMerchant legacy;
        private Player trading;
        private MerchantRecipeList recipes;
        private MerchantOffers offers;

        Adapter(IMerchant legacy) {
            this.legacy = legacy;
        }

        @Override
        public void setTradingPlayer(Player player) {
            this.trading = player;
            legacy.setCustomer(player == null ? null : (EntityPlayer) EntityBridge.wrap(player));
        }

        @Override
        public Player getTradingPlayer() {
            return trading;
        }

        @Override
        public MerchantOffers getOffers() {
            if (offers == null) {
                offers = new MerchantOffers();
                recipes = legacy.getRecipes(trading == null ? null : (EntityPlayer) EntityBridge.wrap(trading));
                if (recipes != null) {
                    for (Object o : recipes) {
                        MerchantOffer offer = o instanceof MerchantRecipe r ? offer(r) : null;
                        // keep indices aligned with the legacy list (a recipe that cannot be shown stays as a used-up offer)
                        offers.add(offer != null ? offer : new MerchantOffer(new ItemCost(net.minecraft.world.item.Items.BARRIER), net.minecraft.world.item.ItemStack.EMPTY, 0, 0, 0.0F));
                    }
                }
            }
            return offers;
        }

        @Override
        public void overrideOffers(MerchantOffers offers) {
        }

        @Override
        public void notifyTrade(MerchantOffer offer) {
            offer.increaseUses();
            int i = offers == null ? -1 : offers.indexOf(offer);
            if (recipes != null && i >= 0 && i < recipes.size() && recipes.get(i) instanceof MerchantRecipe r) legacy.useRecipe(r);
        }

        @Override
        public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack) {
            legacy.verifySellingItem(stack == null || stack.isEmpty() ? null : ItemStack.mcc$wrap(stack));
        }

        @Override
        public int getVillagerXp() {
            return 0;
        }

        @Override
        public void overrideXp(int xp) {
        }

        @Override
        public boolean showProgressBar() {
            return false;
        }

        @Override
        public SoundEvent getNotifyTradeSound() {
            return SoundEvents.VILLAGER_YES;
        }

        @Override
        public boolean isClientSide() {
            return trading == null || trading.level().isClientSide();
        }
    }
}
