package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Render types for legacy geometry drawn after {@code glDisable(GL_DEPTH_TEST)}: the entity types the emulation normally uses always
 * depth-test, so geometry a mod meant to draw over everything (Thaumcraft's Thaumonomicon tab icons at z = -80 behind the tab,
 * node reveal through walls) was hidden.
 */
public final class LegacyRenderTypes extends RenderType {
    private LegacyRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
            Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    private static final Function<ResourceLocation, RenderType> CUTOUT_NO_DEPTH = Util.memoize(texture -> create(
            "legacyconnector_cutout_no_depth", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(true)));

    private static final Function<ResourceLocation, RenderType> TRANSLUCENT_NO_DEPTH = Util.memoize(texture -> create(
            "legacyconnector_translucent_no_depth", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)));

    private static final RenderType UNTEXTURED_NO_DEPTH = create("legacyconnector_untextured_no_depth", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, true,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    /**
     * glBlendFunc(GL_SRC_ALPHA, GL_ONE) with a texture: additive light weighted by alpha (Thaumcraft's aura nodes, beams and
     * glows). The eyes type adds ONE, ONE and ignores alpha, so faint layers stacked into solid white.
     */
    private static final Function<ResourceLocation, RenderType> ADDITIVE_ALPHA = Util.memoize(texture -> create(
            "legacyconnector_additive_alpha", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> ADDITIVE_ALPHA_NO_DEPTH = Util.memoize(texture -> create(
            "legacyconnector_additive_alpha_no_depth", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)));

    /**
     * Additive (GL_SRC_ALPHA, GL_ONE) textured geometry in screens: the eyes shader applies the world's fog, which at GUI
     * depths faded it to black (Thaumcraft's glowing aspect icons in the Thaumonomicon).
     */
    private static final Function<ResourceLocation, RenderType> GUI_ADDITIVE = Util.memoize(texture -> create(
            "legacyconnector_gui_additive", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)));

    /** Unlit textured geometry in screens with GL lighting off (alpha blended): no entity shading, no world fog. */
    private static final Function<ResourceLocation, RenderType> GUI_UNLIT = Util.memoize(texture -> create(
            "legacyconnector_gui_unlit", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)));

    public static RenderType guiUnlit(ResourceLocation texture) {
        return GUI_UNLIT.apply(texture);
    }

    public static RenderType guiAdditive(ResourceLocation texture) {
        return GUI_ADDITIVE.apply(texture);
    }

    public static RenderType additiveAlpha(ResourceLocation texture, boolean depthTest) {
        return depthTest ? ADDITIVE_ALPHA.apply(texture) : ADDITIVE_ALPHA_NO_DEPTH.apply(texture);
    }

    public static RenderType cutoutNoDepth(ResourceLocation texture) {
        return CUTOUT_NO_DEPTH.apply(texture);
    }

    public static RenderType translucentNoDepth(ResourceLocation texture) {
        return TRANSLUCENT_NO_DEPTH.apply(texture);
    }

    public static RenderType untexturedNoDepth() {
        return UNTEXTURED_NO_DEPTH;
    }
}
