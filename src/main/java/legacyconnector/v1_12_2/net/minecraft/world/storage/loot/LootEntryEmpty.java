package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import java.util.Collection;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/** 1.12.2 loot entry that adds nothing (a weighted chance of no loot). */
public class LootEntryEmpty extends LootEntry {
    public LootEntryEmpty(int weight, int quality, LootCondition[] conditions, String entryName) {
        super(weight, quality, conditions, entryName);
    }

    @Override
    public void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
    }
}
