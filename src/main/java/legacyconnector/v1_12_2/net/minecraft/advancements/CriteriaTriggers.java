package legacyconnector.v1_12_2.net.minecraft.advancements;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.advancements.critereon.ConsumeItemTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.critereon.EnchantedItemTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.critereon.EnterBlockTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.critereon.ItemDurabilityTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.critereon.PlacedBlockTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.critereon.SummonedEntityTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.critereon.UsedTotemTrigger;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * The criterion trigger registry. The vanilla triggers mods fire themselves fire the modern triggers; mods' own triggers are kept
 * here by id ({@code get}, {@code getAll}). Mods' advancement files are not loaded by the modern game, so nothing listens to
 * mods' triggers yet.
 */
public class CriteriaTriggers {
    private static final Map<ResourceLocation, ICriterionTrigger<?>> REGISTRY = new LinkedHashMap<>();
    public static final EnterBlockTrigger ENTER_BLOCK = register(new EnterBlockTrigger());
    public static final SummonedEntityTrigger SUMMONED_ENTITY = register(new SummonedEntityTrigger());
    public static final EnchantedItemTrigger ENCHANTED_ITEM = register(new EnchantedItemTrigger());
    public static final ItemDurabilityTrigger ITEM_DURABILITY_CHANGED = register(new ItemDurabilityTrigger());
    public static final ConsumeItemTrigger CONSUME_ITEM = register(new ConsumeItemTrigger());
    public static final UsedTotemTrigger USED_TOTEM = register(new UsedTotemTrigger());
    public static final PlacedBlockTrigger PLACED_BLOCK = register(new PlacedBlockTrigger());

    /** Registers a trigger under its id (1.12.2 threw on a duplicate id; here the first one is kept and the duplicate logged). */
    public static <T extends ICriterionTrigger<?>> T register(T criterion) {
        synchronized (REGISTRY) {
            ResourceLocation id = criterion.getId();
            if (id != null && REGISTRY.containsKey(id)) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").warn("Duplicate criterion id {}; keeping the first", id);
            } else if (id != null) {
                REGISTRY.put(id, criterion);
            }
        }
        return criterion;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ICriterionInstance> ICriterionTrigger<T> get(ResourceLocation id) {
        synchronized (REGISTRY) {
            return (ICriterionTrigger<T>) REGISTRY.get(id);
        }
    }

    public static Iterable<? extends ICriterionTrigger<?>> getAll() {
        synchronized (REGISTRY) {
            return java.util.List.copyOf(REGISTRY.values());
        }
    }
}
