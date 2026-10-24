package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Map;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomChestContent;
import legacyconnector.v1_7_10.net.minecraftforge.common.ChestGenHooks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10 mods added their items to vanilla chests through {@code ChestGenHooks.addItem(category, entry)}. The vanilla chest loot
 * tables are data now, so each category becomes an extra pool on the matching table. 1.7.10 rolled one weighted list (vanilla and
 * modded entries together) a number of times; the extra pool rolls a number of times proportional to the modded share of an
 * assumed vanilla weight of 100, so mod items appear about as often as their weights intend.
 */
public final class ChestLootBridge1710 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/ChestLoot");
    private static final double ASSUMED_VANILLA_WEIGHT = 100.0D;

    /** 1.7.10 category -> loot table of the same structure. */
    private static final Map<String, String> TABLES = Map.ofEntries(
            Map.entry(ChestGenHooks.DUNGEON_CHEST, "minecraft:chests/simple_dungeon"),
            Map.entry(ChestGenHooks.MINESHAFT_CORRIDOR, "minecraft:chests/abandoned_mineshaft"),
            Map.entry(ChestGenHooks.PYRAMID_DESERT_CHEST, "minecraft:chests/desert_pyramid"),
            Map.entry(ChestGenHooks.PYRAMID_JUNGLE_CHEST, "minecraft:chests/jungle_temple"),
            Map.entry(ChestGenHooks.PYRAMID_JUNGLE_DISPENSER, "minecraft:chests/jungle_temple_dispenser"),
            Map.entry(ChestGenHooks.STRONGHOLD_CORRIDOR, "minecraft:chests/stronghold_corridor"),
            Map.entry(ChestGenHooks.STRONGHOLD_LIBRARY, "minecraft:chests/stronghold_library"),
            Map.entry(ChestGenHooks.STRONGHOLD_CROSSING, "minecraft:chests/stronghold_crossing"),
            Map.entry(ChestGenHooks.VILLAGE_BLACKSMITH, "minecraft:chests/village/village_weaponsmith"),
            Map.entry(ChestGenHooks.BONUS_CHEST, "minecraft:chests/spawn_bonus_chest"));

    private ChestLootBridge1710() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener((LootTableLoadEvent e) -> {
            for (var entry : TABLES.entrySet()) {
                if (!e.getName().toString().equals(entry.getValue())) continue;
                try {
                    LootPool pool = pool(entry.getKey());
                    if (pool != null) e.getTable().addPool(pool);
                } catch (RuntimeException | LinkageError ex) {
                    LOG.warn("Could not add the legacy loot of {} to {}: {}", entry.getKey(), entry.getValue(), ex.toString());
                }
            }
        });
    }

    private static LootPool pool(String category) {
        ChestGenHooks info = ChestGenHooks.getInfo(category);
        WeightedRandomChestContent[] items = info.getItems(new Random());
        if (items == null || items.length == 0) return null;
        LootPool.Builder builder = LootPool.lootPool();
        double weight = 0;
        int added = 0;
        for (WeightedRandomChestContent c : items) {
            if (c == null || c.theItemId == null) continue;
            var stack = c.theItemId.mcc$modern();
            if (stack.isEmpty()) continue;
            int min = Math.max(1, c.theMinimumChanceToGenerateItem);
            int max = Math.max(min, c.theMaximumChanceToGenerateItem);
            builder.add(LootItem.lootTableItem(stack.getItem()).setWeight(Math.max(1, c.itemWeight))
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max))));
            weight += Math.max(1, c.itemWeight);
            added++;
        }
        if (added == 0) return null;
        double averageRolls = (info.getMin() + info.getMax()) / 2.0D;
        float expected = (float) (averageRolls * weight / (weight + ASSUMED_VANILLA_WEIGHT));
        LOG.info("Legacy loot for {}: {} entries, about {} rolls", category, added, expected);
        return builder.setRolls(UniformGenerator.between(0.0F, Math.max(1.0F, expected * 2.0F))).build();
    }
}
