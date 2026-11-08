package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;

/**
 * Stands in for {@code AbstractCriterionTriggerInstance} (1.20.1): the base of a custom advancement trigger's condition object. It implements 1.21.1's
 * {@code SimpleInstance}; the JSON side ({@link #serializeToJson}) is what {@link LegacySimpleCriterionTrigger}'s codec calls.
 */
public abstract class LegacyCriterionInstance implements SimpleCriterionTrigger.SimpleInstance {
    private final ResourceLocation criterion;
    private final ContextAwarePredicate player;

    protected LegacyCriterionInstance(ResourceLocation criterion, ContextAwarePredicate player) {
        this.criterion = criterion;
        this.player = player;
    }

    public ResourceLocation getCriterion() {
        return criterion;
    }

    public JsonObject serializeToJson(LegacySerializationContext context) {
        JsonObject json = new JsonObject();
        if (player != null) {
            var element = CritCompat.toJson(player);
            if (element != null && !(element.isJsonArray() && element.getAsJsonArray().isEmpty())) json.add("player", element);
        }
        return json;
    }

    @Override
    public Optional<ContextAwarePredicate> player() {
        return Optional.ofNullable(player);
    }
}
