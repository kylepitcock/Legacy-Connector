package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advancement trigger and predicate calls of 1.20.1: the JSON readers and writers ({@code ItemPredicate.fromJson}, {@code ContextAwarePredicate.toJson}...) became
 * codecs in 1.21.1. The helpers run a codec over JSON with the ops of the decode in progress ({@link LegacySimpleCriterionTrigger} sets them), else the running
 * server's registries, else plain JSON.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class CritCompat {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/triggers");
    static final ThreadLocal<DynamicOps<?>> CURRENT = new ThreadLocal<>();

    private CritCompat() {}

    static DynamicOps<?> ops() {
        DynamicOps<?> ops = CURRENT.get();
        if (ops != null) return ops;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        RegistryAccess access = server != null ? server.registryAccess() : null;
        return access != null ? RegistryOps.create(JsonOps.INSTANCE, access) : JsonOps.INSTANCE;
    }

    public static <T> T decode(Codec<T> codec, JsonElement json) {
        if (json == null || json.isJsonNull()) return null;
        return decodeWith(codec, json, ops());
    }

    private static <T, U> T decodeWith(Codec<T> codec, JsonElement json, DynamicOps<U> ops) {
        U converted = JsonOps.INSTANCE.convertTo(ops, json);
        var result = codec.parse(ops, converted);
        if (result.error().isPresent()) {
            LOG.warn("Cannot read advancement JSON {}: {}", json, result.error().get().message());
            return null;
        }
        return result.result().orElse(null);
    }

    public static <T> JsonElement encode(Codec<T> codec, T value) {
        return encodeWith(codec, value, ops());
    }

    private static <T, U> JsonElement encodeWith(Codec<T> codec, T value, DynamicOps<U> ops) {
        var result = codec.encodeStart(ops, value);
        if (result.error().isPresent()) return null;
        U out = result.result().orElse(null);
        return out == null ? null : ops.convertTo(JsonOps.INSTANCE, out);
    }

    public static JsonElement toJson(ContextAwarePredicate predicate) {
        return encode(ContextAwarePredicate.CODEC, predicate);
    }

    // ---- ContextAwarePredicate

    public static ContextAwarePredicate anyPlayer() {
        return ContextAwarePredicate.create();
    }

    public static JsonElement contextToJson(ContextAwarePredicate predicate, LegacySerializationContext context) {
        JsonElement e = toJson(predicate);
        return e == null ? new JsonArray() : e;
    }

    public static ContextAwarePredicate entityFromJson(JsonObject json, String name, LegacyDeserializationContext context) {
        ContextAwarePredicate p = json.has(name) ? decode(ContextAwarePredicate.CODEC, json.get(name)) : null;
        return p == null ? anyPlayer() : p;
    }

    public static ContextAwarePredicate fromElement(String name, LegacyDeserializationContext context, JsonElement element, net.minecraft.world.level.storage.loot.parameters.LootContextParamSet params) {
        ContextAwarePredicate p = decode(ContextAwarePredicate.CODEC, element);
        return p == null ? anyPlayer() : p;
    }

    // ---- ItemPredicate, EntityPredicate, LocationPredicate: "ANY" and JSON

    public static ItemPredicate anyItem() {
        return ItemPredicate.Builder.item().build();
    }

    public static EntityPredicate anyEntity() {
        return EntityPredicate.Builder.entity().build();
    }

    public static LocationPredicate anyLocation() {
        return LocationPredicate.Builder.location().build();
    }

    public static ItemPredicate itemFromJson(JsonElement json) {
        ItemPredicate p = decode(ItemPredicate.CODEC, json);
        return p == null ? anyItem() : p;
    }

    public static JsonElement itemToJson(ItemPredicate predicate) {
        return encode(ItemPredicate.CODEC, predicate);
    }

    public static ItemPredicate[] itemsFromJsonArray(JsonElement json) {
        if (json == null || json.isJsonNull()) return new ItemPredicate[0];
        JsonArray array = json.getAsJsonArray();
        ItemPredicate[] out = new ItemPredicate[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = itemFromJson(array.get(i));
        return out;
    }

    public static EntityPredicate entityPredicateFromJson(JsonElement json) {
        EntityPredicate p = decode(EntityPredicate.CODEC, json);
        return p == null ? anyEntity() : p;
    }

    public static JsonElement entityPredicateToJson(EntityPredicate predicate) {
        return encode(EntityPredicate.CODEC, predicate);
    }

    public static LocationPredicate locationFromJson(JsonElement json) {
        LocationPredicate p = decode(LocationPredicate.CODEC, json);
        return p == null ? anyLocation() : p;
    }

    public static JsonElement locationToJson(LocationPredicate predicate) {
        return encode(LocationPredicate.CODEC, predicate);
    }

    public static boolean matches(ItemPredicate predicate, ItemStack stack) {
        return predicate.test(stack);
    }

    /** Used by {@link LegacySimpleCriterionTrigger}'s codec: the plain JSON object of an instance written in 1.21.1 ops. */
    static JsonObject asObject(JsonElement e) {
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    /**
     * {@code codec()} of a trigger that implements {@code CriterionTrigger} directly (1.20.1 had {@code createInstance(JsonObject, DeserializationContext)}
     * and {@code getId()} on the interface): reads the conditions as JSON and calls the mod's method.
     */
    public static Codec directCodec(Object trigger) {
        java.lang.reflect.Method create = null;
        for (java.lang.reflect.Method m : trigger.getClass().getMethods())
            if (m.getName().equals("createInstance") && m.getParameterCount() == 2 && JsonObject.class.isAssignableFrom(m.getParameterTypes()[0])
                    && !m.isBridge() && !m.isSynthetic()) create = m;
        final java.lang.reflect.Method createFinal = create;
        String id;
        try {
            id = String.valueOf(trigger.getClass().getMethod("getId").invoke(trigger));
        } catch (ReflectiveOperationException | RuntimeException e) {
            id = trigger.getClass().getName();
        }
        final String name = id;
        return new Codec<Object>() {
            @Override
            public <U> com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<Object, U>> decode(DynamicOps<U> ops, U input) {
                if (createFinal == null) return com.mojang.serialization.DataResult.error(() -> "Trigger " + name + " has no createInstance");
                DynamicOps<?> before = CURRENT.get();
                CURRENT.set(ops);
                try {
                    JsonObject json = asObject(ops.convertTo(JsonOps.INSTANCE, input));
                    Object instance = createFinal.invoke(trigger, json, new LegacyDeserializationContext(net.minecraft.resources.ResourceLocation.parse(name)));
                    return com.mojang.serialization.DataResult.success(com.mojang.datafixers.util.Pair.of(instance, ops.empty()));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    return com.mojang.serialization.DataResult.error(() -> "Cannot read the conditions of trigger " + name + ": " + e);
                } finally {
                    CURRENT.set(before);
                    if (before == null) CURRENT.remove();
                }
            }

            @Override
            public <U> com.mojang.serialization.DataResult<U> encode(Object instance, DynamicOps<U> ops, U prefix) {
                try {
                    JsonObject json = (JsonObject) instance.getClass().getMethod("serializeToJson", LegacySerializationContext.class).invoke(instance, LegacySerializationContext.INSTANCE);
                    return com.mojang.serialization.DataResult.success(JsonOps.INSTANCE.convertTo(ops, (JsonElement) json));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    return com.mojang.serialization.DataResult.error(() -> "Cannot write the conditions of trigger " + name + ": " + e);
                }
            }
        };
    }
}
