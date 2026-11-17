package legacyconnector.v1_12_2.net.minecraftforge.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import legacyconnector.v1_12_2.net.minecraft.advancements.critereon.ItemPredicate;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** Forge's registry of custom item predicates: a name an advancement's {@code "type"} may use, and the factory that reads it from JSON. */
public class ItemPredicates {
    private static final Map<ResourceLocation, Function<JsonObject, ItemPredicate>> PREDICATES = new LinkedHashMap<>();

    static {
        register(new ResourceLocation("forge", "ore_dict"), json -> new OredictItemPredicate(json.get("ore").getAsString()));
    }

    /** Registers a predicate under a name; a second one under the same name is ignored. */
    public static synchronized void register(ResourceLocation rl, Function<JsonObject, ItemPredicate> factory) {
        if (rl == null || factory == null) return;
        if (PREDICATES.containsKey(rl)) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/ItemPredicates").warn("An item predicate with name {} is already registered; keeping the first", rl);
            return;
        }
        PREDICATES.put(rl, factory);
    }

    public static synchronized Function<JsonObject, ItemPredicate> get(ResourceLocation rl) {
        return PREDICATES.get(rl);
    }
}
