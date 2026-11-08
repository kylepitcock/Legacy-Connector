package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

/** The client-only half of {@link ApiCompat} (a separate class so that a dedicated server never needs the client classes to verify the common helpers). */
public final class ApiCompatClient {
    private ApiCompatClient() {}

    /** {@code ModelEvent.RegisterGeometryLoaders.register(String, loader)}: the loader's name is in the namespace of the registering mod. */
    public static void registerGeometryLoader(ModelEvent.RegisterGeometryLoaders event, String name, IGeometryLoader<?> loader) {
        event.register(ResourceLocation.fromNamespaceAndPath(ModLoadingContext.get().getActiveNamespace(), name), loader);
    }

    /** {@code ModelEvent.RegisterAdditional.register(ResourceLocation)}. */
    public static void registerAdditionalModel(ModelEvent.RegisterAdditional event, ResourceLocation model) {
        event.register(ModelResourceLocation.standalone(model));
    }

    /** {@code RenderLevelStageEvent.getPartialTick()}: the float of the render partial tick. */
    public static float partialTick(RenderLevelStageEvent event) {
        DeltaTracker tracker = event.getPartialTick();
        return tracker.getGameTimeDeltaPartialTick(false);
    }
}
