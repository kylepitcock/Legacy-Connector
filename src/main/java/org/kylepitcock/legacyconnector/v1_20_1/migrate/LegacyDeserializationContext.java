package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Stands in for {@code DeserializationContext} (1.20.1), the JSON reader handed to {@code createInstance}. */
public class LegacyDeserializationContext {
    private final ResourceLocation id;

    public LegacyDeserializationContext(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getAdvancementId() {
        return id;
    }

    public LootItemCondition[] deserializeConditions(JsonArray array, String name, LootContextParamSet params) {
        List<LootItemCondition> out = new ArrayList<>();
        for (JsonElement e : array) {
            LootItemCondition c = CritCompat.decode(LootItemCondition.DIRECT_CODEC, e);
            if (c != null) out.add(c);
        }
        return out.toArray(new LootItemCondition[0]);
    }
}
