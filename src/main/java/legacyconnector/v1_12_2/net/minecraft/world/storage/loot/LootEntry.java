package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import java.util.Collection;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/** 1.12.2 loot pool entry: a weighted, named choice with conditions that adds stacks when picked. */
public abstract class LootEntry {
    protected final String entryName;
    protected final int weight;
    protected final int quality;
    protected final LootCondition[] conditions;

    protected LootEntry(int weight, int quality, LootCondition[] conditions, String entryName) {
        this.weight = weight;
        this.quality = quality;
        this.conditions = conditions == null ? new LootCondition[0] : conditions;
        this.entryName = entryName;
    }

    public int getEffectiveWeight(float luck) {
        return Math.max((int) Math.floor(weight + quality * luck), 0);
    }

    public String getEntryName() {
        return entryName;
    }

    public abstract void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context);

    protected void serialize(com.google.gson.JsonObject json, com.google.gson.JsonSerializationContext context) {
    }

    /** Gson adapter for loot entries in table JSON (the vanilla "item", "loot_table" and "empty" types). */
    public static class Serializer implements com.google.gson.JsonDeserializer<LootEntry>, com.google.gson.JsonSerializer<LootEntry> {
        @Override
        public LootEntry deserialize(com.google.gson.JsonElement element, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) {
            var json = element.getAsJsonObject();
            String kind = json.has("type") ? json.get("type").getAsString() : "item";
            int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
            int quality = json.has("quality") ? json.get("quality").getAsInt() : 0;
            String name = json.has("entryName") ? json.get("entryName").getAsString() : json.has("name") ? json.get("name").getAsString() : kind;
            LootCondition[] conditions = new LootCondition[0];
            switch (kind) {
                case "item": {
                    var item = legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId(json.get("name").getAsString());
                    return new LootEntryItem(item, weight, quality, new legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.LootFunction[0], conditions, name);
                }
                case "loot_table":
                    return new LootEntryTable(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(json.get("name").getAsString()), weight, quality, conditions,
                            name);
                case "empty":
                    return new LootEntryEmpty(weight, quality, conditions, name);
                default:
                    throw new com.google.gson.JsonSyntaxException("Unknown loot entry type " + kind);
            }
        }

        @Override
        public com.google.gson.JsonElement serialize(LootEntry entry, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext context) {
            var json = new com.google.gson.JsonObject();
            if (entry.entryName != null) json.addProperty("entryName", entry.entryName);
            json.addProperty("type", entry instanceof LootEntryTable ? "loot_table" : entry instanceof LootEntryEmpty ? "empty" : "item");
            json.addProperty("weight", entry.weight);
            json.addProperty("quality", entry.quality);
            entry.serialize(json, context);
            return json;
        }
    }
}
