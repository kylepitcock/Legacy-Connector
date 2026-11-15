package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.Type;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

/**
 * Stands in for {@code net.minecraft.world.level.storage.loot.Deserializers} (1.20.1): the Gson builders that read and write loot tables and loot functions as
 * JSON. 1.21.1 reads them through codecs, so each loot type gets a Gson adapter over its codec, resolved against the registries of the running game
 * (enchantments and the like).
 */
public final class LegacyLootDeserializers {
    private LegacyLootDeserializers() {}

    private static final class CodecAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
        private final Codec<T> codec;

        CodecAdapter(Codec<T> codec) {
            this.codec = codec;
        }

        private static DynamicOps<JsonElement> ops() {
            return RegistryOps.create(JsonOps.INSTANCE, MigrateContext.registries());
        }

        @Override
        public JsonElement serialize(T src, Type type, JsonSerializationContext context) {
            return codec.encodeStart(ops(), src).getOrThrow(msg -> new JsonParseException(String.valueOf(msg)));
        }

        @Override
        public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return codec.parse(ops(), json).getOrThrow(msg -> new JsonParseException(String.valueOf(msg)));
        }
    }

    private static <T> void add(GsonBuilder builder, Class<T> type, Codec<T> codec) {
        builder.registerTypeHierarchyAdapter(type, new CodecAdapter<>(codec));
    }

    /** {@code Deserializers.createFunctionSerializer()}: functions, conditions and number providers. */
    public static GsonBuilder createFunctionSerializer() {
        GsonBuilder b = new GsonBuilder();
        add(b, LootItemFunction.class, LootItemFunctions.ROOT_CODEC);
        add(b, LootItemCondition.class, LootItemCondition.DIRECT_CODEC);
        add(b, NumberProvider.class, NumberProviders.CODEC);
        return b;
    }

    /** {@code Deserializers.createConditionSerializer()}: conditions and number providers. */
    public static GsonBuilder createConditionSerializer() {
        GsonBuilder b = new GsonBuilder();
        add(b, LootItemCondition.class, LootItemCondition.DIRECT_CODEC);
        add(b, NumberProvider.class, NumberProviders.CODEC);
        return b;
    }

    /** {@code Deserializers.createLootTableSerializer()}: loot tables, pools and entries on top of the function serializer. */
    public static GsonBuilder createLootTableSerializer() {
        GsonBuilder b = createFunctionSerializer();
        add(b, LootTable.class, LootTable.DIRECT_CODEC);
        add(b, LootPool.class, LootPool.CODEC);
        add(b, LootPoolEntryContainer.class, LootPoolEntries.CODEC);
        return b;
    }
}
