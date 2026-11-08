package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Stands in for {@code LootItemConditionalFunction.Serializer} (1.20.1): reads the {@code conditions} array itself and hands the function the parsed array, as
 * the old base class did. The conditions are written by {@link LootCompat} (1.21.1 keeps them in the function object), so {@link #serialize} has nothing to add.
 */
public abstract class LegacyConditionalFunctionSerializer<T extends LootItemConditionalFunction> implements LegacyLootSerializer<T> {
    @Override
    public void serialize(JsonObject json, T value, JsonSerializationContext context) {}

    @Override
    public final T deserialize(JsonObject json, JsonDeserializationContext context) {
        return deserialize(json, context, LootCompat.conditions(json));
    }

    public abstract T deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions);
}
