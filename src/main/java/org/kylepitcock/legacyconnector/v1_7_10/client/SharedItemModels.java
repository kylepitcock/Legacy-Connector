package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.LoggerFactory;

/**
 * perf.shareItemModels: legacy items whose generated item model files are identical (the subtype items {@code mod:name_N} of an item
 * that draws every damage value with the same icon, the block items of blocks sharing one model, 1.12 subtypes naming the same model
 * file) keep one baked model between them. The model bakery bakes each item model on its own: an {@code item/generated} model of a
 * 16x16 icon is a few hundred quads (one per pixel edge), 20 to 50 KB each, so a pack with thousands of legacy subtype items held
 * the same quads thousands of times. The duplicates are dropped after baking (the same model draws, pixel for pixel).
 *
 * <p>Steps aside with ModernFix: its dynamic resources bake item models on demand, and reading every model here would bake them all.
 */
public final class SharedItemModels {
    private static final Map<ResourceLocation, String> FILES = new ConcurrentHashMap<>();
    private static final boolean ENABLED = org.kylepitcock.legacyconnector.core.LegacyConfig.perfShareItemModels()
            && !net.neoforged.fml.ModList.get().isLoaded("modernfix");
    private static boolean registered;

    private SharedItemModels() {}

    public static synchronized void register(IEventBus modBus) {
        if (registered || !ENABLED) return;
        registered = true;
        // before the legacy model bake events (1.12 mods replace baked models by location there) and other mods' wrappers
        modBus.addListener(EventPriority.HIGH, SharedItemModels::onModifyBaking);
    }

    /** The content of the generated model file of an item ({@code models/item/<path>.json}). */
    public static void note(ResourceLocation item, String json) {
        if (ENABLED && item != null && json != null) FILES.put(item, json);
    }

    private static void onModifyBaking(ModelEvent.ModifyBakingResult event) {
        if (FILES.isEmpty()) return;
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        Map<String, BakedModel> byContent = new HashMap<>();
        int shared = 0;
        for (var e : FILES.entrySet()) {
            ModelResourceLocation location = ModelResourceLocation.inventory(e.getKey());
            BakedModel model = models.get(location);
            if (model == null || !plain(model)) continue;
            BakedModel first = byContent.putIfAbsent(e.getValue(), model);
            if (first != null && first != model && first.getClass() == model.getClass()) {
                models.put(location, first);
                shared++;
            }
        }
        FILES.clear(); // filled again when the pack is generated for the next reload
        if (shared > 0) LoggerFactory.getLogger("LegacyConnector/Models").info("{} legacy item models are identical to another and share its baked model", shared);
    }

    /** Only the bakery's own models (another mod's wrapper may hold state of its item). */
    private static boolean plain(BakedModel model) {
        return model instanceof net.minecraft.client.resources.model.SimpleBakedModel || model instanceof net.minecraft.client.resources.model.BuiltInModel;
    }
}
