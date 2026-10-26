package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * A condition of a loot table entry, pool or function. Its test, {@code testCondition(Random, LootContext)}, names the synthesized
 * {@code LootContext}, so implementations declare it on their own and it is reached through the synthesized helpers.
 */
public interface LootCondition {
    /** Reads and writes a condition type from and to loot table JSON; registered with {@link LootConditionManager}. */
    abstract class Serializer<T extends LootCondition> {
        private final ResourceLocation lootTableLocation;
        private final Class<T> conditionClass;

        protected Serializer(ResourceLocation location, Class<T> clazz) {
            this.lootTableLocation = location;
            this.conditionClass = clazz;
        }

        public ResourceLocation getLootTableLocation() {
            return lootTableLocation;
        }

        public Class<T> getConditionClass() {
            return conditionClass;
        }

        public abstract void serialize(JsonObject json, T value, JsonSerializationContext context);

        public abstract T deserialize(JsonObject json, JsonDeserializationContext context);
    }
}
