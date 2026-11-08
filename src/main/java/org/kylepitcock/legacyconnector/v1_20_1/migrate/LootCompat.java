package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Loot functions and conditions registered by a 1.20.1 mod carry a JSON {@link LegacyLootSerializer}; 1.21.1 wants a map codec. The codec built here hands the
 * serializer the JSON object of the entry (best effort: the context deserializes plain Gson types, nested loot objects come as their JSON).
 */
public final class LootCompat {
    private static final Gson GSON = new GsonBuilder().create();

    private LootCompat() {}

    private static final JsonDeserializationContext READ = new JsonDeserializationContext() {
        @Override
        public <R> R deserialize(JsonElement json, Type type) throws JsonParseException {
            return GSON.fromJson(json, type);
        }
    };

    private static final JsonSerializationContext WRITE = new JsonSerializationContext() {
        @Override
        public JsonElement serialize(Object src) {
            return GSON.toJsonTree(src);
        }

        @Override
        public JsonElement serialize(Object src, Type type) {
            return GSON.toJsonTree(src, type);
        }
    };

    /** {@code new LootItemFunctionType(serializer)} and {@code new LootItemConditionType(serializer)}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MapCodec codec(LegacyLootSerializer serializer) {
        return new MapCodec<Object>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.empty();
            }

            @Override
            public <T> DataResult<Object> decode(DynamicOps<T> ops, MapLike<T> input) {
                JsonObject json = new JsonObject();
                input.entries().forEach(entry -> ops.getStringValue(entry.getFirst()).result()
                        .ifPresent(key -> json.add(key, ops.convertTo(JsonOps.INSTANCE, entry.getSecond()))));
                try {
                    return DataResult.success(serializer.deserialize(json, READ));
                } catch (RuntimeException e) {
                    return DataResult.error(() -> "Could not read " + serializer.getClass().getName() + ": " + e);
                }
            }

            @Override
            public <T> RecordBuilder<T> encode(Object value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                JsonObject json = new JsonObject();
                try {
                    serializer.serialize(json, value, WRITE);
                } catch (RuntimeException e) {
                    return prefix.withErrorsFrom(DataResult.error(() -> "Could not write " + serializer.getClass().getName() + ": " + e));
                }
                for (var e : json.entrySet()) prefix.add(e.getKey(), JsonOps.INSTANCE.convertTo(ops, e.getValue()));
                return prefix;
            }
        };
    }

    /** The {@code conditions} array of a loot function JSON, parsed. */
    public static LootItemCondition[] conditions(JsonObject json) {
        List<LootItemCondition> out = new ArrayList<>();
        if (json.has("conditions") && json.get("conditions").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("conditions")) {
                LootItemCondition.DIRECT_CODEC.parse(JsonOps.INSTANCE, el).result().ifPresent(out::add);
            }
        }
        return out.toArray(new LootItemCondition[0]);
    }
}
