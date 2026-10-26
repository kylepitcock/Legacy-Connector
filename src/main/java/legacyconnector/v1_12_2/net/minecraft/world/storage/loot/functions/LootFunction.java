package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/**
 * A function applied to the stack a loot entry produces, under its conditions. {@code apply(ItemStack, Random, LootContext)} names the
 * synthesized {@code LootContext}, so subclasses declare it on their own and it is reached through the synthesized helpers.
 */
public abstract class LootFunction {
    private final LootCondition[] conditions;

    protected LootFunction(LootCondition[] conditions) {
        this.conditions = conditions;
    }

    public LootCondition[] getConditions() {
        return conditions;
    }

    /** Reads and writes a function type from and to loot table JSON; registered with {@link LootFunctionManager}. */
    public abstract static class Serializer<T extends LootFunction> {
        private final ResourceLocation lootTableLocation;
        private final Class<T> functionClass;

        protected Serializer(ResourceLocation location, Class<T> clazz) {
            this.lootTableLocation = location;
            this.functionClass = clazz;
        }

        public ResourceLocation getFunctionName() {
            return lootTableLocation;
        }

        public Class<T> getFunctionClass() {
            return functionClass;
        }

        public abstract void serialize(JsonObject object, T function, JsonSerializationContext context);

        public abstract T deserialize(JsonObject object, JsonDeserializationContext context, LootCondition[] conditions);
    }
}
