package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Stands in for {@code LootPoolEntryContainer.Serializer} (1.20.1): the JSON reader/writer of a loot pool entry type a mod registers with
 * {@code new LootPoolEntryType(serializer)} (which {@link LootCompat} turns into a map codec). Reads the {@code conditions} of the entry as the original did
 * and hands the rest to the mod's {@link #deserializeCustom}.
 */
public abstract class LegacyLootEntrySerializer<T extends LootPoolEntryContainer> implements LegacyLootSerializer<T> {
    public abstract void serializeCustom(JsonObject json, T value, JsonSerializationContext context);

    public abstract T deserializeCustom(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions);

    @Override
    public final void serialize(JsonObject json, T value, JsonSerializationContext context) {
        List<LootItemCondition> conditions = LegacyLootFields.list(value, LootPoolEntryContainer.class, "conditions");
        if (conditions != null && !conditions.isEmpty()) {
            JsonArray array = new JsonArray();
            for (LootItemCondition c : conditions) LootItemCondition.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, c).result().ifPresent(array::add);
            json.add("conditions", array);
        }
        serializeCustom(json, value, context);
    }

    @Override
    public final T deserialize(JsonObject json, JsonDeserializationContext context) {
        return deserializeCustom(json, context, LootCompat.conditions(json));
    }
}
