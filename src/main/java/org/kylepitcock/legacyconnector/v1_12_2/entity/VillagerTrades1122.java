package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.VillagerRegistry;
import org.slf4j.LoggerFactory;

/**
 * Trades mods add to the vanilla careers ({@code VillagerCareer.addTrade}) become modern villager trades of the matching profession and
 * level (career levels past five join level five). Each legacy trade list is asked for a trade whenever a villager rolls a new offer.
 */
public final class VillagerTrades1122 {
    private VillagerTrades1122() {}

    public static void register(net.neoforged.bus.api.IEventBus game) {
        game.addListener((net.neoforged.neoforge.event.village.VillagerTradesEvent e) -> {
            for (VillagerRegistry.VillagerProfession profession : VillagerRegistry.mcc$professions()) {
                if (profession.mcc$modern() != e.getType()) continue;
                for (VillagerRegistry.VillagerCareer career : profession.mcc$careers()) {
                    for (int level = 0; level < career.mcc$levels(); level++) {
                        var trades = career.getTrades(level);
                        if (trades == null) continue;
                        int modernLevel = Math.min(5, level + 1);
                        for (EntityVillager.ITradeList trade : trades) e.getTrades().get(modernLevel).add(listing(trade));
                    }
                }
            }
        });
    }

    /** A modern trade listing that asks the legacy trade list for one trade. */
    static net.minecraft.world.entity.npc.VillagerTrades.ItemListing listing(EntityVillager.ITradeList trade) {
        return (trader, random) -> {
            try {
                MerchantRecipeList list = new MerchantRecipeList();
                legacyconnector.v1_12_2.net.minecraft.entity.IMerchant merchant = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(trader)
                        instanceof legacyconnector.v1_12_2.net.minecraft.entity.IMerchant m ? m : null;
                trade.addMerchantRecipe(merchant, list, new java.util.Random(random.nextLong()));
                return list.isEmpty() ? null : list.get(0).mcc$toModern();
            } catch (RuntimeException | LinkageError ex) {
                LoggerFactory.getLogger("LegacyConnector/Villagers").debug("A 1.12.2 trade list failed: {}", ex.toString());
                return null;
            }
        };
    }
}
