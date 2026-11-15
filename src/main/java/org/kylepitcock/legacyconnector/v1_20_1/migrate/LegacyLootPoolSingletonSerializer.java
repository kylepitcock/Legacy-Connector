package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Stands in for {@code LootPoolSingletonContainer.Serializer} (1.20.1), the base of the entry serializers of item-like entries: it reads and writes the
 * {@code weight}, {@code quality} and {@code functions} the original handled, and gives the mod's six-argument {@code deserialize} the rest of the JSON.
 */
public abstract class LegacyLootPoolSingletonSerializer<T extends LootPoolSingletonContainer> extends LegacyLootEntrySerializer<T> {
    @Override
    public void serializeCustom(JsonObject json, T value, JsonSerializationContext context) {
        int weight = LegacyLootFields.integer(value, LootPoolSingletonContainer.class, "weight", 1);
        int quality = LegacyLootFields.integer(value, LootPoolSingletonContainer.class, "quality", 0);
        if (weight != 1) json.addProperty("weight", weight);
        if (quality != 0) json.addProperty("quality", quality);
        List<LootItemFunction> functions = LegacyLootFields.list(value, LootPoolSingletonContainer.class, "functions");
        if (functions != null && !functions.isEmpty()) {
            JsonArray array = new JsonArray();
            for (LootItemFunction f : functions) LootItemFunctions.ROOT_CODEC.encodeStart(JsonOps.INSTANCE, f).result().ifPresent(array::add);
            json.add("functions", array);
        }
    }

    @Override
    public final T deserializeCustom(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions) {
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        int quality = json.has("quality") ? json.get("quality").getAsInt() : 0;
        List<LootItemFunction> functions = new ArrayList<>();
        if (json.has("functions") && json.get("functions").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("functions")) LootItemFunctions.ROOT_CODEC.parse(JsonOps.INSTANCE, el).result().ifPresent(functions::add);
        }
        return deserialize(json, context, weight, quality, conditions, functions.toArray(new LootItemFunction[0]));
    }

    protected abstract T deserialize(JsonObject json, JsonDeserializationContext context, int weight, int quality, LootItemCondition[] conditions,
                                     LootItemFunction[] functions);
}
