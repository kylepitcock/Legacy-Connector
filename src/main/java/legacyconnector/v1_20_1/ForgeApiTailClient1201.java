package legacyconnector.v1_20_1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModLoadingContext;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterNamedRenderTypesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Client-only part of {@link ForgeApiTail1201} (separate so a dedicated server never loads the client event classes); reached with a {@code TailClient.} prefix. */
public final class ForgeApiTailClient1201 {
    private ForgeApiTailClient1201() {}


    // ---- client events

    private static float partial(DeltaTracker tracker) {
        return tracker.getGameTimeDeltaPartialTick(false);
    }

    public static float guiPartialTick(RenderGuiEvent event) {
        return partial(event.getPartialTick());
    }

    public static float highlightPartialTick(RenderHighlightEvent event) {
        return partial(event.getDeltaTracker());
    }

    public static com.mojang.blaze3d.platform.Window guiWindow(RenderGuiEvent event) {
        return Minecraft.getInstance().getWindow();
    }

    public static double scrollDelta(ScreenEvent.MouseScrolled event) {
        return event.getScrollDeltaY();
    }

    /** The debug screen lists are ArrayLists in practice; a different list type is copied (and edits to the copy are lost). */
    public static ArrayList<String> debugLeft(CustomizeGuiOverlayEvent.DebugText event) {
        List<String> l = event.getLeft();
        return l instanceof ArrayList<String> a ? a : new ArrayList<>(l);
    }

    public static ArrayList<String> debugRight(CustomizeGuiOverlayEvent.DebugText event) {
        List<String> l = event.getRight();
        return l instanceof ArrayList<String> a ? a : new ArrayList<>(l);
    }

    // ---- audit of the client events (docs/wiki/Legacy-1.20.1-Client-Events.md): float -> DeltaTracker, String -> ResourceLocation, renamed getters

    public static float customizePartialTick(CustomizeGuiOverlayEvent event) {
        return partial(event.getPartialTick());
    }

    public static float customizePartialTick(CustomizeGuiOverlayEvent.BossEventProgress e) { return customizePartialTick((CustomizeGuiOverlayEvent) e); }

    public static float customizePartialTick(CustomizeGuiOverlayEvent.Chat e) { return customizePartialTick((CustomizeGuiOverlayEvent) e); }

    public static float customizePartialTick(CustomizeGuiOverlayEvent.DebugText e) { return customizePartialTick((CustomizeGuiOverlayEvent) e); }

    public static float highlightPartialTick(RenderHighlightEvent.Entity e) { return highlightPartialTick((RenderHighlightEvent) e); }

    public static float levelStagePartialTick(RenderLevelStageEvent event) {
        return partial(event.getPartialTick());
    }

    /** 1.20.1 had one scroll value (the vertical one); 1.21.1 has an axis each. */
    public static double inputScrollDelta(InputEvent.MouseScrollingEvent event) {
        return event.getScrollDeltaY();
    }

    public static boolean pressedHandled(ScreenEvent.MouseButtonPressed.Post event) {
        return event.wasClickHandled();
    }

    public static boolean releasedHandled(ScreenEvent.MouseButtonReleased.Post event) {
        return event.wasReleaseHandled();
    }

    // the entity renderer lookups of EntityRenderersEvent.AddLayers: 1.20.1 returned LivingEntityRenderer from the living ones and named the player skins by String

    @SuppressWarnings("rawtypes")
    public static EntityRenderer addLayersEntityRenderer(EntityRenderersEvent.AddLayers event, EntityType type) {
        return event.getRenderer(type);
    }

    /** The living renderer of the type, null when its renderer is not one (as 1.20.1's cast-free lookup returned null for those). */
    @SuppressWarnings("rawtypes")
    public static LivingEntityRenderer addLayersLivingRenderer(EntityRenderersEvent.AddLayers event, EntityType type) {
        return event.getRenderer(type) instanceof LivingEntityRenderer<?, ?> living ? living : null;
    }

    @SuppressWarnings("rawtypes")
    public static EntityRenderer addLayersPlayerSkin(EntityRenderersEvent.AddLayers event, String skin) {
        return event.getSkin(PlayerSkin.Model.byName(skin));
    }

    @SuppressWarnings("rawtypes")
    public static LivingEntityRenderer addLayersLivingSkin(EntityRenderersEvent.AddLayers event, String skin) {
        return event.getSkin(PlayerSkin.Model.byName(skin)) instanceof LivingEntityRenderer<?, ?> living ? living : null;
    }

    // the registrations that took a bare name in 1.20.1 and a ResourceLocation now: the name lives in the namespace of the mod being loaded

    private static ResourceLocation activeName(String name) {
        return name.indexOf(':') >= 0 ? ResourceLocation.parse(name) : ResourceLocation.fromNamespaceAndPath(ModLoadingContext.get().getActiveNamespace(), name);
    }

    /** {@code ModelEvent.RegisterAdditional.register(ResourceLocation)}: the model is a standalone one in 1.21.1. */
    public static void registerAdditionalModel(ModelEvent.RegisterAdditional event, ResourceLocation model) {
        org.kylepitcock.legacyconnector.v1_20_1.migrate.ModelLocationCompat.registerAdditional(event, model);
    }

    public static void registerGeometryLoader(ModelEvent.RegisterGeometryLoaders event, String name, IGeometryLoader<?> loader) {
        event.register(activeName(name), loader);
    }

    public static void registerNamedRenderType(RegisterNamedRenderTypesEvent event, String name, RenderType block, RenderType entity) {
        event.register(activeName(name), block, entity);
    }

    public static void registerNamedRenderType(RegisterNamedRenderTypesEvent event, String name, RenderType block, RenderType entity, RenderType fabulous) {
        event.register(activeName(name), block, entity, fabulous);
    }

    // ---- exact-owner overloads (the rewritten call passes the receiver typed as the class the mod called it on)

    public static float guiPartialTick(RenderGuiEvent.Post e) { return guiPartialTick((RenderGuiEvent) e); }

    public static float guiPartialTick(RenderGuiEvent.Pre e) { return guiPartialTick((RenderGuiEvent) e); }

    public static com.mojang.blaze3d.platform.Window guiWindow(RenderGuiEvent.Post e) { return guiWindow((RenderGuiEvent) e); }

    public static com.mojang.blaze3d.platform.Window guiWindow(RenderGuiEvent.Pre e) { return guiWindow((RenderGuiEvent) e); }

    public static float highlightPartialTick(RenderHighlightEvent.Block e) { return highlightPartialTick((RenderHighlightEvent) e); }

    public static double scrollDelta(ScreenEvent.MouseScrolled.Pre e) { return scrollDelta((ScreenEvent.MouseScrolled) e); }

    public static double scrollDelta(ScreenEvent.MouseScrolled.Post e) { return scrollDelta((ScreenEvent.MouseScrolled) e); }}
