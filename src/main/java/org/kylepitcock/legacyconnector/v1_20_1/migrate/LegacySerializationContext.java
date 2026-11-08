package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Stands in for {@code SerializationContext} (1.20.1), the JSON writer handed to {@code serializeToJson}. */
public class LegacySerializationContext {
    public static final LegacySerializationContext INSTANCE = new LegacySerializationContext();

    public JsonElement conditionsToJson(LootItemCondition[] conditions) {
        JsonArray array = new JsonArray();
        for (LootItemCondition c : conditions) {
            JsonElement e = CritCompat.encode(LootItemCondition.DIRECT_CODEC, c);
            if (e != null) array.add(e);
        }
        return array;
    }
}
