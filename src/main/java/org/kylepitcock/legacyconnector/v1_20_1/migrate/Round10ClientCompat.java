package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * Client-side helpers of gap round 10 (1.20.1 mods on 1.21.1). Reached from {@code mc-calls.tsv}, {@code mc-fields.tsv} and {@code mc-bridges.tsv}.
 */
public final class Round10ClientCompat {
    private Round10ClientCompat() {}

    // ---- particle render types ---------------------------------------------------------------------------------------------------------------

    /**
     * {@code ParticleRenderType.begin(BufferBuilder, TextureManager)} called by a mod: 1.21.1 starts the builder on the tesselator and answers it, the 1.20.1
     * builder object (see {@link CompatBufferBuilder}) takes that builder over, so the mod's {@code builder.end()} / {@code tesselator.end()} finish it.
     */
    public static void particleBegin(ParticleRenderType type, CompatBufferBuilder builder, TextureManager textures) {
        Tesselator tesselator = builder.tesselator() != null ? builder.tesselator() : Tesselator.getInstance();
        BufferBuilder started = type.begin(tesselator, textures);
        if (started != null) builder.adopt(started);
    }

    /**
     * {@code ParticleRenderType.end(Tesselator)} called by a mod: draws what {@link #particleBegin} (or the type's begin) started. A mod type's own
     * {@code end} body is not run (1.21.1 has no such hook; the particle engine draws the mesh itself, APPROXIMATION: a mod's extra state reset in end is skipped).
     */
    public static void particleEnd(ParticleRenderType type, Tesselator tesselator) {
        if (CompatBufferBuilder.startedOf(tesselator) != null) CompatBufferBuilder.end(tesselator);
    }

    // ---- camera -------------------------------------------------------------------------------------------------------------------------------

    /**
     * {@code Camera.getMaxZoom(double)}: the third-person camera distance shortened so the camera does not end up inside blocks. 1.21.1 keeps the
     * same method with a float parameter and result (made public by the layer's access transformer).
     */
    public static double cameraMaxZoom(Camera camera, double distance) {
        return camera.getMaxZoom((float) distance);
    }

    // ---- vertex formats -----------------------------------------------------------------------------------------------------------------------

    /**
     * {@code DefaultVertexFormat.ELEMENT_PADDING} / {@code VertexFormatElement.Usage.PADDING}: 1.21.1 has no padding element (a format gets padding through
     * {@code VertexFormat.Builder.padding(int)}). The element is a marker no format contains; {@link MiddleClientCompat#newVertexFormat} turns it into
     * one byte of padding. APPROXIMATION: the marker uses the {@code GENERIC} usage, so code that compares a usage with {@code PADDING} sees GENERIC.
     */
    public static final VertexFormatElement PADDING_ELEMENT = new VertexFormatElement(31, 31, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, 1);

    public static VertexFormatElement elementPadding() {
        return PADDING_ELEMENT;
    }

    public static VertexFormatElement.Usage usagePadding() {
        return VertexFormatElement.Usage.GENERIC;
    }

    /** {@code new VertexFormatElement(index, type, usage, count)} of 1.20.1: the id is taken from the next free slot of the 1.21.1 registry (or an existing equal element). */
    public static VertexFormatElement newElement(int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        // Usage.PADDING reads as GENERIC (see usagePadding), so a byte element of that usage is the padding element (APPROXIMATION: a genuine generic byte element is padding too)
        if (usage == VertexFormatElement.Usage.GENERIC && type == VertexFormatElement.Type.BYTE) return PADDING_ELEMENT;
        for (int id = 0; id < VertexFormatElement.MAX_COUNT; id++) {
            VertexFormatElement existing = VertexFormatElement.byId(id);
            if (existing != null && existing.index() == index && existing.type() == type && existing.usage() == usage && existing.count() == count) return existing;
        }
        int id = VertexFormatElement.findNextId();
        return VertexFormatElement.register(id, index, type, usage, count);
    }

    /** True for the {@link #PADDING_ELEMENT} marker. */
    public static boolean isPadding(VertexFormatElement element) {
        return element == PADDING_ELEMENT;
    }

    static void addTo(VertexFormat.Builder builder, String name, VertexFormatElement element) {
        if (isPadding(element)) builder.padding(1);
        else builder.add(name, element);
    }

    // ---- screens ------------------------------------------------------------------------------------------------------------------------------

    /** {@code new BookViewScreen.WrittenBookAccess(stack)}: the written-book view of an item stack is {@code BookAccess.fromItem} now. */
    public static net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess bookAccess(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess.fromItem(stack);
    }

    /** {@code JoinMultiplayerScreen.setToolTip(List<Component>)}: the tooltip is drawn in the next render pass (1.21.1's {@code setTooltipForNextRenderPass}). */
    public static void setToolTip(net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen screen, java.util.List<net.minecraft.network.chat.Component> lines) {
        if (lines == null) return;
        screen.setTooltipForNextRenderPass(lines.stream().map(net.minecraft.network.chat.Component::getVisualOrderText).toList());
    }

    // ---- models, recipe book, maps ------------------------------------------------------------------------------------------------------------

    /** {@code new ModelBakery.ModelBakerImpl(bakery, spriteGetter, location)}: the sprite getter takes the model location as 1.21.1's {@code TextureGetter} passes it. */
    public static net.minecraft.client.resources.model.ModelBakery.ModelBakerImpl newModelBaker(net.minecraft.client.resources.model.ModelBakery bakery,
            java.util.function.BiFunction<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.Material,
                    net.minecraft.client.renderer.texture.TextureAtlasSprite> spriteGetter, net.minecraft.resources.ResourceLocation location) {
        net.minecraft.client.resources.model.ModelBakery.TextureGetter getter = (modelLocation, material) -> spriteGetter.apply(modelLocation.id(), material);
        return bakery.new ModelBakerImpl(getter, net.minecraft.client.resources.model.ModelResourceLocation.standalone(location));
    }

    /** {@code GhostRecipe.setRecipe(Recipe)}: 1.21.1 takes the recipe with its id (a stand-in id: the ghost recipe only reads the recipe). */
    public static void ghostSetRecipe(net.minecraft.client.gui.screens.recipebook.GhostRecipe ghost, net.minecraft.world.item.crafting.Recipe<?> recipe) {
        ghost.setRecipe(new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "ghost"), recipe));
    }

    /** {@code MapRenderer.render(pose, buffers, int mapId, data, active, light)}: map ids are {@code MapId} objects now. */
    public static void mapRender(net.minecraft.client.gui.MapRenderer renderer, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
            int mapId, net.minecraft.world.level.saveddata.maps.MapItemSavedData data, boolean active, int light) {
        renderer.render(pose, buffers, new net.minecraft.world.level.saveddata.maps.MapId(mapId), data, active, light);
    }

    /**
     * {@code new RenderLevelStageEvent(stage, renderer, pose, projection, tick, partialTick, camera, frustum)}: 1.21.1 also carries the model-view matrix (taken
     * from the pose stack) and a {@code DeltaTracker} (a fixed one with the partial tick).
     */
    public static net.neoforged.neoforge.client.event.RenderLevelStageEvent newRenderLevelStageEvent(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage stage,
            net.minecraft.client.renderer.LevelRenderer renderer, com.mojang.blaze3d.vertex.PoseStack pose, org.joml.Matrix4f projection, int tick, float partialTick,
            Camera camera, net.minecraft.client.renderer.culling.Frustum frustum) {
        net.minecraft.client.DeltaTracker tracker = new net.minecraft.client.DeltaTracker() {
            @Override
            public float getGameTimeDeltaTicks() {
                return partialTick;
            }

            @Override
            public float getGameTimeDeltaPartialTick(boolean ignoreFrozenGame) {
                return partialTick;
            }

            @Override
            public float getRealtimeDeltaTicks() {
                return partialTick;
            }
        };
        return new net.neoforged.neoforge.client.event.RenderLevelStageEvent(stage, renderer, pose, new org.joml.Matrix4f(pose.last().pose()), projection, tick, tracker, camera, frustum);
    }

    /**
     * {@code BannerRenderer.renderPatterns(pose, buffers, light, overlay, flag, material, banner, List<Pair<Holder<BannerPattern>, DyeColor>>)}: the 1.20.1 list has the
     * base colour as its first layer (as {@code Round9Compat.createPatterns} builds it); 1.21.1 takes the base colour and the pattern layers apart.
     */
    public static void bannerRenderPatterns(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay,
            net.minecraft.client.model.geom.ModelPart flag, net.minecraft.client.resources.model.Material material, boolean banner,
            java.util.List<com.mojang.datafixers.util.Pair<net.minecraft.core.Holder<net.minecraft.world.level.block.entity.BannerPattern>, net.minecraft.world.item.DyeColor>> patterns) {
        net.minecraft.world.item.DyeColor base = patterns == null || patterns.isEmpty() ? net.minecraft.world.item.DyeColor.WHITE : patterns.get(0).getSecond();
        java.util.List<net.minecraft.world.level.block.entity.BannerPatternLayers.Layer> layers = new java.util.ArrayList<>();
        if (patterns != null) {
            for (int i = 1; i < patterns.size(); i++) {
                layers.add(new net.minecraft.world.level.block.entity.BannerPatternLayers.Layer(patterns.get(i).getFirst(), patterns.get(i).getSecond()));
            }
        }
        net.minecraft.client.renderer.blockentity.BannerRenderer.renderPatterns(pose, buffers, light, overlay, flag, material, banner, base,
                new net.minecraft.world.level.block.entity.BannerPatternLayers(layers));
    }
}
