package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;

/**
 * Stands in for {@code SimpleCriterionTrigger} (1.20.1) as the base of a mod's custom advancement trigger: the mod writes {@code createInstance(JsonObject,
 * ContextAwarePredicate, DeserializationContext)} and {@code serializeToJson}, 1.21.1 wants a {@code Codec}. The codec here reads the conditions as JSON, finds the
 * {@code player} predicate, and hands both to the mod's method.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class LegacySimpleCriterionTrigger<T extends LegacyCriterionInstance> extends SimpleCriterionTrigger<T> {
    private Codec<T> codec;

    public abstract ResourceLocation getId();

    protected abstract T createInstance(JsonObject json, ContextAwarePredicate player, LegacyDeserializationContext context);

    @Override
    public final Codec<T> codec() {
        if (codec == null) codec = new Codec<T>() {
            @Override
            public <U> DataResult<Pair<T, U>> decode(DynamicOps<U> ops, U input) {
                DynamicOps<?> before = CritCompat.CURRENT.get();
                CritCompat.CURRENT.set(ops);
                try {
                    JsonObject json = CritCompat.asObject(ops.convertTo(JsonOps.INSTANCE, input));
                    ContextAwarePredicate player = null;
                    if (json.has("player")) player = CritCompat.decode(ContextAwarePredicate.CODEC, json.get("player"));
                    if (player == null) player = CritCompat.anyPlayer();
                    T instance = createInstance(json, player, new LegacyDeserializationContext(getId()));
                    return DataResult.success(Pair.of(instance, ops.empty()));
                } catch (RuntimeException e) {
                    return DataResult.error(() -> "Cannot read the conditions of trigger " + getId() + ": " + e);
                } finally {
                    CritCompat.CURRENT.set(before);
                    if (before == null) CritCompat.CURRENT.remove();
                }
            }

            @Override
            public <U> DataResult<U> encode(T instance, DynamicOps<U> ops, U prefix) {
                try {
                    JsonObject json = instance.serializeToJson(LegacySerializationContext.INSTANCE);
                    U converted = JsonOps.INSTANCE.convertTo(ops, (JsonElement) json);
                    return DataResult.success(converted);
                } catch (RuntimeException e) {
                    return DataResult.error(() -> "Cannot write the conditions of trigger " + getId() + ": " + e);
                }
            }
        };
        return codec;
    }
}
