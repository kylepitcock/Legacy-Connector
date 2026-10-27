package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import java.util.Collection;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/** 1.12.2 loot entry that rolls another loot table (by name, from the server's tables). */
public class LootEntryTable extends LootEntry {
    protected final ResourceLocation table;

    public LootEntryTable(ResourceLocation table, int weight, int quality, LootCondition[] conditions, String entryName) {
        super(weight, quality, conditions, entryName);
        this.table = table;
    }

    @Override
    public void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
        LootTableManager manager = context.getLootTableManager();
        if (manager == null) manager = LootTableManager.mcc$of(net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer());
        if (manager == null) return;
        stacks.addAll(manager.getLootTableFromLocation(table).generateLootForPools(rand, context));
    }

    public static LootEntryTable deserialize(com.google.gson.JsonObject object, com.google.gson.JsonDeserializationContext context, int weight, int quality,
            LootCondition[] conditions) {
        String name = object.get("name").getAsString();
        return new LootEntryTable(new ResourceLocation(name), weight, quality, conditions, name);
    }
}
