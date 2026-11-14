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
    /**
     * No depth test, for real: vanilla's NO_DEPTH_TEST (GL_ALWAYS) leaves the depth test as it finds it, relying on every
     * other type switching it off afterwards. Legacy code draws straight to GL as well (blits with GL_DEPTH_TEST on), and
     * geometry meant to ignore depth then failed against it (Thaumonomicon category icons behind their tabs).
     */
    private static final DepthTestStateShard LEGACY_NO_DEPTH = new DepthTestStateShard("legacyconnector_no_depth", 519) {
        @Override
        public void setupRenderState() {
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        }
    };

    private LegacyRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
            Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    private static final Function<ResourceLocation, RenderType> CUTOUT_NO_DEPTH = Util.memoize(texture -> batchingClass(create(
            "legacyconnector_cutout_no_depth", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setDepthTestState(LEGACY_NO_DEPTH)
                    .createCompositeState(true)), "WATER_MASK"));

    private static final Function<ResourceLocation, RenderType> TRANSLUCENT_NO_DEPTH = Util.memoize(texture -> batchingClass(create(
            "legacyconnector_translucent_no_depth", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setDepthTestState(LEGACY_NO_DEPTH)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)), "WATER_MASK"));

    private static final RenderType UNTEXTURED_NO_DEPTH = batchingClass(create("legacyconnector_untextured_no_depth", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, true,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEGACY_NO_DEPTH)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)), "WATER_MASK");

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

    /**
     * glBlendFunc(GL_ONE, GL_ONE) with a texture in the world: glow layers of mob models. 1.7.10 drew them with its default alpha test
     * (texels below 0.1 alpha dropped), and such textures keep white RGB in their transparent texels; the eyes type adds ONE, ONE and
     * ignores alpha, so the whole model turned white. This type drops the transparent texels (the cutout shader) and adds the rest.
     */
    private static final Function<ResourceLocation, RenderType> ADDITIVE_CUTOUT = Util.memoize(texture -> create(
            "legacyconnector_additive_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)));

    public static RenderType additiveCutout(ResourceLocation texture) {
        return ADDITIVE_CUTOUT.apply(texture);
    }

    private static final Function<ResourceLocation, RenderType> ADDITIVE_ALPHA_NO_DEPTH = Util.memoize(texture -> batchingClass(create(
            "legacyconnector_additive_alpha_no_depth", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEGACY_NO_DEPTH)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)), "WATER_MASK"));

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

    private static RenderType legacyLines(boolean depthTest) {
        return create(depthTest ? "legacyconnector_lines" : "legacyconnector_lines_no_depth", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES, 1536, false, false,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(java.util.OptionalDouble.empty()))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setDepthTestState(depthTest ? LEQUAL_DEPTH_TEST : LEGACY_NO_DEPTH)
                        .createCompositeState(false));
    }

    private static final RenderType LINES = batchingClass(legacyLines(true), "LINES");
    private static final RenderType LINES_NO_DEPTH = batchingClass(legacyLines(false), "WATER_MASK");

    /**
     * Iris's batched entity rendering (with a shader pack) draws render types sorted by a class it derives from vanilla state
     * shards: vanilla's no-depth types (NO_DEPTH_TEST) and "lines" are drawn after everything else. The legacy no-depth types use
     * a state shard of their own and the lines type another name, so they fell into the opaque or general translucent class and
     * were drawn before geometry they were meant to cover. They get the class of their vanilla counterparts.
     */
    private static RenderType batchingClass(RenderType type, String irisClass) {
        org.kylepitcock.legacyconnector.compat.IrisState.setBatchingClass(type, irisClass);
        return type;
    }

    // ---------------------------------------------------------------- shader packs (Iris)
    // Iris picks a gbuffers program by the shader a render type sets (GameRenderer's getters, which it overrides) and the phase.
    // The emulation's normal types use the entity shaders, which Iris maps to gbuffers_entities / gbuffers_block (lit, shadowed,
    // fogged as a solid object). With a shader pack in use, legacy geometry that is not an object takes the shader of its vanilla
    // equivalent instead: sky geometry the position_tex_color / position_color shaders (gbuffers_skytextured / skybasic during
    // the sky phase), particles the particle shader (gbuffers_particles / weather), additive glows the beacon beam shader
    // (gbuffers_beaconbeam in every phase; the eyes shader becomes the lit gbuffers_block while block entities draw).

    /** Legacy sky geometry: unlit, no depth writes (1.7.10's renderSky ran with glDepthMask(false)). */
    private static final java.util.function.BiFunction<ResourceLocation, Boolean, RenderType> SKY_TEXTURED = Util.memoize((texture, blend) -> create(
            "legacyconnector_sky_textured", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(blend ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)));

    private static final RenderType SKY_UNTEXTURED = create("legacyconnector_sky_untextured", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    /** Legacy particles as vanilla's particle sheets draw: the particle shader, lightmap, depth writes, both sides. */
    private static final java.util.function.BiFunction<ResourceLocation, Boolean, RenderType> PARTICLE = Util.memoize((texture, blend) -> create(
            "legacyconnector_particle", DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS, 1536, false, blend,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getParticleShader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(blend ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)));

    private record GlowKey(ResourceLocation texture, boolean alphaWeighted, boolean depthTest) {}

    /** Additive light (GL_SRC_ALPHA or GL_ONE, GL_ONE) through the beacon beam shader: no depth writes, both sides. */
    private static final Function<GlowKey, RenderType> GLOW = Util.memoize(k -> {
        RenderType type = create(k.depthTest() ? "legacyconnector_glow" : "legacyconnector_glow_no_depth", DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS, 1536, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                        .setTextureState(new TextureStateShard(k.texture(), false, false))
                        .setTransparencyState(k.alphaWeighted() ? LIGHTNING_TRANSPARENCY : ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(k.depthTest() ? LEQUAL_DEPTH_TEST : LEGACY_NO_DEPTH)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
        return k.depthTest() ? type : batchingClass(type, "WATER_MASK");
    });

    public static RenderType skyTextured(ResourceLocation texture, boolean blend) {
        return SKY_TEXTURED.apply(texture, blend);
    }

    public static RenderType skyUntextured() {
        return SKY_UNTEXTURED;
    }

    public static RenderType particle(ResourceLocation texture, boolean blend) {
        return PARTICLE.apply(texture, blend);
    }

    /** Additive textured light under a shader pack; {@code alphaWeighted}: GL_SRC_ALPHA, GL_ONE rather than GL_ONE, GL_ONE. */
    public static RenderType glow(ResourceLocation texture, boolean alphaWeighted, boolean depthTest) {
        return GLOW.apply(new GlowKey(texture, alphaWeighted, depthTest));
    }

    /** GL_LINES in the world, one pixel wide (the vanilla line shader widens them in screen space). */
    public static RenderType lines(boolean depthTest) {
        return depthTest ? LINES : LINES_NO_DEPTH;
    }

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
