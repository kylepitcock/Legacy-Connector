package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The loot condition types by name and by class. */
public class LootConditionManager {
    private static final Map<ResourceLocation, LootCondition.Serializer<?>> NAME_TO_SERIALIZER_MAP = new HashMap<>();
    private static final Map<Class<?>, LootCondition.Serializer<?>> CLASS_TO_SERIALIZER_MAP = new HashMap<>();

    public static synchronized <T extends LootCondition> void registerCondition(LootCondition.Serializer<? extends T> serializer) {
        ResourceLocation name = serializer.getLootTableLocation();
        Class<?> clazz = serializer.getConditionClass();
        if (NAME_TO_SERIALIZER_MAP.containsKey(name)) throw new IllegalArgumentException("Can't re-register item condition name " + name);
        if (CLASS_TO_SERIALIZER_MAP.containsKey(clazz)) throw new IllegalArgumentException("Can't re-register item condition class " + clazz.getName());
        NAME_TO_SERIALIZER_MAP.put(name, serializer);
        CLASS_TO_SERIALIZER_MAP.put(clazz, serializer);
    }

    public static synchronized LootCondition.Serializer<?> getSerializerForName(ResourceLocation location) {
        LootCondition.Serializer<?> serializer = NAME_TO_SERIALIZER_MAP.get(location);
        if (serializer == null) throw new IllegalArgumentException("Unknown loot item condition '" + location + "'");
        return serializer;
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends LootCondition> LootCondition.Serializer<T> getSerializerFor(T condition) {
        LootCondition.Serializer<?> serializer = condition == null ? null : CLASS_TO_SERIALIZER_MAP.get(condition.getClass());
        if (serializer == null) throw new IllegalArgumentException("Unknown loot item condition " + condition);
        return (LootCondition.Serializer<T>) serializer;
    }

    /** Whether every condition passes (an empty or null list passes). */
    public static boolean testAllConditions(LootCondition[] conditions, java.util.Random rand, legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext context) {
        if (conditions == null) return true;
        for (LootCondition c : conditions) if (c != null && !c.testCondition(rand, context)) return false;
        return true;
    }

    public static boolean testAllConditions(Iterable<LootCondition> conditions, java.util.Random rand, legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext context) {
        if (conditions == null) return true;
        for (LootCondition c : conditions) if (c != null && !c.testCondition(rand, context)) return false;
        return true;
    }

    /** Gson adapter for conditions in loot table JSON ("condition" names the registered serializer). */
    public static class Serializer implements com.google.gson.JsonDeserializer<LootCondition>, com.google.gson.JsonSerializer<LootCondition> {
        @Override
        public LootCondition deserialize(com.google.gson.JsonElement element, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) {
            var json = element.getAsJsonObject();
            var name = new ResourceLocation(json.get("condition").getAsString());
            return getSerializerForName(name).deserialize(json, context);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public com.google.gson.JsonElement serialize(LootCondition condition, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext context) {
            LootCondition.Serializer s = getSerializerFor(condition);
            var json = new com.google.gson.JsonObject();
            s.serialize(json, condition, context);
            json.addProperty("condition", s.getLootTableLocation().toString());
            return json;
        }
    }
}
