package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

/**
 * Stands in for {@code net.minecraft.world.level.storage.loot.Serializer} (1.20.1): the JSON reader/writer a mod passed to {@code new LootItemFunctionType(..)} or
 * {@code new LootItemConditionType(..)}. 1.21.1 uses map codecs; {@link LootCompat} builds one over this object.
 */
public interface LegacyLootSerializer<T> {
    void serialize(JsonObject json, T value, JsonSerializationContext context);

    T deserialize(JsonObject json, JsonDeserializationContext context);
}
