package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/**
 * The "OpenGL state" legacy rendering code believes it is manipulating.
 *
 * <p>1.7.10 renderers drew with immediate-mode OpenGL: push a matrix, translate/rotate, bind a texture, set a color,
 * draw. Minecraft 1.21 renders with a core-profile pipeline where those calls do not exist. While a legacy
 * renderer runs, this context points at the modern target (a {@link GuiGraphics} for screens, or a
 * {@link PoseStack} plus {@link MultiBufferSource} for tile entity/item rendering) and the shim's GL11, texture
 * manager and model classes translate each legacy call into operations on it.</p>
 *
 * <p>The emulated state is per thread ({@link State}): the render thread draws while chunk-meshing threads capture legacy block
 * renderers into baked quads, and each sees only its own frames, colour, texture, enables, matrix modes and immediate-mode
 * vertices (the Tessellator is per thread too, {@code Tessellator.mcc$get$instance}). The render thread therefore never waits for a
 * chunk thread. Captures still run one at a time ({@link #CAPTURE_LOCK}): legacy block renderers were written for 1.7.10's single
 * chunk-rendering thread and keep scratch state in static fields of their own. Contexts nest (a TESR may render an item that uses
 * another renderer).</p>
 */
public final class RenderContext {
    private record Frame(GuiGraphics gui, PoseStack pose, MultiBufferSource buffers, int light, int overlay, PoseStack.Pose base, boolean locked) {}

    private static final ResourceLocation UNKNOWN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static final int GL_MODELVIEW = 5888, GL_PROJECTION = 5889, GL_TEXTURE = 5890;

    /** Everything a legacy renderer can change, for one thread. */
    static final class State {
        final Deque<Frame> frames = new ArrayDeque<>();
        Capture capture;
        /** glBegin/glEnd geometry (GL11's immediate mode). */
        final LegacyVertices immediate = new LegacyVertices();
        /** {@code glPolygonMode(..., GL_LINE)}: filled primitives are drawn as their edges (CustomOreGen's wireframes). */
        boolean lineMode;
        ResourceLocation texture = UNKNOWN_TEXTURE;
        float r = 1, g = 1, b = 1, a = 1;
        /** Matrices pushed by legacy code outside any context (should be rare): count only, so pops stay balanced. */
        int strayPushes;
        boolean texture2d = true;
        boolean blend;
        boolean depthTest = true;
        boolean additive;
        /** Additive with the source weighted by alpha (GL_SRC_ALPHA, GL_ONE) rather than GL_ONE, GL_ONE. */
        boolean additiveAlpha;
        boolean lighting = true;
        int forcedLight = -1;
        /** The emulated active texture unit, relative to GL_TEXTURE0. */
        int activeUnit;
        /** GL_TEXTURE_2D on the lightmap unit: off, the lightmap was not applied and legacy geometry drew full bright. */
        boolean lightmap = true;
        boolean legacyDepthTest = true;
        int blendSrc = 770, blendDst = 771; // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
        int depthFunc = 515; // GL_LEQUAL
        boolean depthWrite = true;
        int colorWrite = 0xF; // r, g, b, a bits
        long quadCount;
        double[] bounds;
        boolean flatGuiQuad;
        boolean armorLayering;
        final ArrayDeque<Attribs> attribs = new ArrayDeque<>();
        int matrixMode = GL_MODELVIEW;
        /** The emulated texture matrix applied to legacy texture coordinates; null is the identity. */
        org.joml.Matrix4f textureMatrix;
        final Deque<org.joml.Matrix4f> textureStack = new ArrayDeque<>();
        /** The legacy projection while legacy code has replaced the modern one; null while it has not. */
        org.joml.Matrix4f projection;
        final Deque<org.joml.Matrix4f> projectionStack = new ArrayDeque<>();
        org.joml.Matrix4f savedProjection;
        com.mojang.blaze3d.vertex.VertexSorting savedSorting;
        org.joml.Matrix4f savedModelView;
        PoseStack.Pose foldedPose;
        /** Scratch for {@link #quad}: the corners with the texture matrix applied. */
        final double[][] uvScratch = {new double[8], new double[8], new double[8], new double[8]};
        final org.joml.Vector4f uv = new org.joml.Vector4f();
        final org.joml.Vector3f normal = new org.joml.Vector3f();
        /** Scratch for {@link #rotate} (PoseStack.mulPose does not keep it). */
        final Quaternionf rotation = new Quaternionf();
        /** Set while LegacyVertices emits a draw: the state is fixed for its quads, so their buffer is looked up once ({@link #drawBuffer}). */
        boolean inDraw;
        VertexConsumer drawBuffer;
        /** While a draw is emitted: the colour its corners without a colour of their own take ({@link #color(State, double[])}). */
        boolean drawColorSet;
        long drawColor;
    }

    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

    /**
     * Captures (chunk-meshing threads, and the render thread's own synchronous rebuilds) run one at a time, as legacy block renderers
     * did on 1.7.10's single chunk thread. Reentrant for nested frames on one thread.
     */
    private static final java.util.concurrent.locks.ReentrantLock CAPTURE_LOCK = new java.util.concurrent.locks.ReentrantLock();
    /**
     * {@code -Dlegacyconnector.renderLock=true}: the render thread takes {@link #CAPTURE_LOCK} for every legacy render too, as
     * before the state was made per thread (a mod whose block renderer and tile/item renderer share static scratch state).
     */
    private static final boolean RENDER_LOCK = Boolean.getBoolean("legacyconnector.renderLock");

    /** Geometry drawn while capturing: quads of {x, y, z, u, v, argb, light, own colour} corners (flat) and their face normals. */
    public static final class Capture {
        /** Doubles per corner, corners per quad. */
        public static final int STRIDE = 8, CORNERS = 4;
        private double[] corners = new double[STRIDE * CORNERS * 16];
        private float[] normals = new float[3 * 16];
        private int quads;

        public int size() {
            return quads;
        }

        public boolean isEmpty() {
            return quads == 0;
        }

        /** Component {@code k} of corner {@code i} of quad {@code q}. */
        public double get(int q, int i, int k) {
            return corners[(q * CORNERS + i) * STRIDE + k];
        }

        public float normal(int q, int k) {
            return normals[q * 3 + k];
        }

        /** Whether corner {@code i} of quad {@code q} carries its own colour. */
        public boolean ownColor(int q, int i) {
            return corners[(q * CORNERS + i) * STRIDE + 7] != 0.0D;
        }

        private int add() {
            if ((quads + 1) * STRIDE * CORNERS > corners.length) corners = java.util.Arrays.copyOf(corners, corners.length * 2);
            if ((quads + 1) * 3 > normals.length) normals = java.util.Arrays.copyOf(normals, normals.length * 2);
            return quads++;
        }
    }

    static State state() {
        return STATE.get();
    }

    private static Deque<Frame> frames() {
        return STATE.get().frames;
    }

    private RenderContext() {}

    private static boolean lockRender() {
        if (!RENDER_LOCK) return false;
        CAPTURE_LOCK.lock();
        return true;
    }

    public static void beginGui(GuiGraphics gui) {
        boolean locked = lockRender();
        State s = STATE.get();
        if (s.frames.isEmpty()) legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.mcc$get$instance().mcc$reset();
        s.frames.push(new Frame(gui, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, gui.pose().last(), locked));
        resetColor(s);
    }

    public static void beginWorld(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        boolean locked = lockRender();
        State s = STATE.get();
        if (s.frames.isEmpty()) legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.mcc$get$instance().mcc$reset();
        s.frames.push(new Frame(null, pose, buffers, light, overlay, pose.last(), locked));
        resetColor(s);
    }

    public static void end() {
        State s = STATE.get();
        Frame f = s.frames.pop();
        try {
            if (s.frames.isEmpty()) {
                // a legacy projection left in place must not outlive the frame (restored while the frame's target is current)
                s.frames.push(f);
                try {
                    resetMatrixModes(s);
                } finally {
                    s.frames.pop();
                }
                legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.mcc$get$instance().mcc$reset();
                resetState();
                // a depth function legacy code left set must not leak into modern rendering
                if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            }
            // Legacy code that pushed more matrices than it popped (or threw midway) must not corrupt the caller's stack.
            // (a legacy renderer that popped too much can leave the stack empty: last() would throw, so check clear() first)
            for (int guard = 0; !f.pose.clear() && f.pose.last() != f.base && guard < 64; guard++) f.pose.popPose();
            if (f.gui != null) f.gui.setColor(1, 1, 1, 1);
            resetColor(s);
        } finally {
            if (f.locked) CAPTURE_LOCK.unlock();
        }
    }

    /**
     * Starts capturing geometry instead of drawing it (a block rendered into a baked model). Positions come out in
     * the space of the emulated matrix stack, which starts as the identity.
     */
    public static Capture beginCapture() {
        CAPTURE_LOCK.lock();
        // a chunk-meshing thread draws into a Tessellator of its own (the render thread keeps the shared instance)
        if (!RenderSystem.isOnRenderThread()) legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.mcc$bindThread();
        State s = STATE.get();
        PoseStack pose = new PoseStack();
        s.frames.push(new Frame(null, pose, null, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose.last(), true));
        resetColor(s);
        Capture c = new Capture();
        s.capture = c;
        return c;
    }

    public static void endCapture() {
        STATE.get().capture = null;
        end();
    }

    public static boolean active() {
        return !frames().isEmpty();
    }

    public static GuiGraphics gui() {
        Frame f = frames().peek();
        return f == null ? null : f.gui;
    }

    static GuiGraphics gui(State s) {
        Frame f = s.frames.peek();
        return f == null ? null : f.gui;
    }

    public static PoseStack pose() {
        Frame f = frames().peek();
        return f == null ? null : f.pose;
    }

    private static PoseStack pose(State s) {
        Frame f = s.frames.peek();
        return f == null ? null : f.pose;
    }

    public static MultiBufferSource buffers() {
        return buffers(STATE.get());
    }

    private static MultiBufferSource buffers(State s) {
        Frame f = s.frames.peek();
        if (f != null) return f.buffers;
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    public static int light() {
        Frame f = frames().peek();
        return f == null ? LightTexture.FULL_BRIGHT : f.light;
    }

    public static int overlay() {
        Frame f = frames().peek();
        return f == null ? OverlayTexture.NO_OVERLAY : f.overlay;
    }

    /** This thread's glBegin/glEnd vertices (GL11's immediate mode). */
    public static LegacyVertices immediate() {
        return STATE.get().immediate;
    }

    /** {@code glPolygonMode(..., GL_LINE)} on this thread. */
    public static boolean lineMode() {
        return STATE.get().lineMode;
    }

    public static void setLineMode(boolean on) {
        STATE.get().lineMode = on;
    }

    // ------------------------------------------------------------------ texture and color

    private static final java.util.Set<ResourceLocation> CHECKED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Logs (once) a texture a legacy renderer binds that no resource pack has: it draws as the missing texture. */
    private static void checkExists(ResourceLocation rl) {
        if (!CHECKED.add(rl) || rl.getPath().startsWith("textures/atlas/") || rl.getPath().startsWith("dynamic/")) return;
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getResourceManager().getResource(rl).isEmpty() && mc.getTextureManager().getTexture(rl, null) == null) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Textures").warn("A legacy renderer binds the texture {}, which does not exist", rl);
            }
        } catch (RuntimeException | LinkageError e) {
            // not checkable now
        }
    }

    public static void bindTexture(ResourceLocation rl) {
        // the resources being reloaded are not visible yet while the pack is built (mods bind textures in TextureStitchEvent.Pre)
        if (org.kylepitcock.legacyconnector.core.pack.GeneratedPack.building()) return;
        checkExists(rl);
        STATE.get().texture = rl;
        RenderSystem.setShaderTexture(0, rl);
    }

    public static ResourceLocation texture() {
        return STATE.get().texture;
    }

    /** glColor4f: components are clamped to [0, 1] as GL did (Thaumcraft's crystals pass 255 / 220; unclamped they wrapped dark). */
    public static void color(float red, float green, float blue, float alpha) {
        red = Math.max(0.0F, Math.min(1.0F, red));
        green = Math.max(0.0F, Math.min(1.0F, green));
        blue = Math.max(0.0F, Math.min(1.0F, blue));
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        State s = STATE.get();
        s.r = red;
        s.g = green;
        s.b = blue;
        s.a = alpha;
        GuiGraphics gui = gui(s);
        if (gui != null) gui.setColor(red, green, blue, alpha);
    }

    private static void resetColor(State s) {
        s.r = s.g = s.b = s.a = 1;
    }

    public static int argb() {
        return argb(STATE.get());
    }

    static int argb(State s) {
        return ((int) (s.a * 255) & 255) << 24 | ((int) (s.r * 255) & 255) << 16 | ((int) (s.g * 255) & 255) << 8 | ((int) (s.b * 255) & 255);
    }

    // ------------------------------------------------------------------ emulated GL state and raw geometry

    public static void setTexture2d(boolean on) {
        STATE.get().texture2d = on;
    }

    // Texture units. 1.7.10 drew with two: GL_TEXTURE0 (the bound texture) and GL_TEXTURE1 (the lightmap), and legacy code
    // selects one with OpenGlHelper.setActiveTexture / GL13.glActiveTexture before switching GL_TEXTURE_2D or binding. The
    // vanilla idiom (EntityRenderer.disableLightmap, DragonAPI's ReikaRenderHelper.disableEntityLighting) is "lightmap unit,
    // glDisable(GL_TEXTURE_2D), default unit": full-bright drawing. Without units it switched texturing off on the main
    // texture, and ChromatiCraft's Crystal Pylon halo (atlas sprite, additive) drew as an untextured opaque white square.

    /** glActiveTexture(GL_TEXTURE0 + n): later GL_TEXTURE_2D switches and binds apply to that unit. */
    public static void setActiveTexture(int unit) {
        STATE.get().activeUnit = Math.max(0, Math.min(31, unit - 33984)); // GL_TEXTURE0
    }

    /** Whether the active unit is the main texture's (binds and GL_TEXTURE_2D act on the drawn texture). */
    public static boolean onDefaultTextureUnit() {
        return STATE.get().activeUnit == 0;
    }

    /** GL_TEXTURE_2D switched on the active unit: the main texture on unit 0, the lightmap on unit 1, ignored elsewhere. */
    public static void setUnitTexture2d(boolean on) {
        State s = STATE.get();
        if (s.activeUnit == 0) s.texture2d = on;
        else if (s.activeUnit == 1) s.lightmap = on;
    }

    /** glIsEnabled(GL_TEXTURE_2D) for the active unit. */
    public static boolean unitTexture2d() {
        State s = STATE.get();
        return s.activeUnit == 0 ? s.texture2d : s.activeUnit == 1 && s.lightmap;
    }

    public static void setBlend(boolean on) {
        State s = STATE.get();
        if (on == s.blend) return;
        if (gui(s) != null && (s.blendSrc != 770 || s.blendDst != 771)) flushBatch(s);
        s.blend = on;
    }

    /**
     * Flushes a screen's batched geometry and puts the emulated depth test back: modern render types switch depth testing
     * on while they draw, and a legacy screen that runs with it off (1.7.10's default) must stay that way. Blending is put
     * back too: the render types switch it off when done, and a blit after them lost its alpha (Thaumonomicon tab shadows
     * drew solid black).
     */
    public static void mcc$flushGui(GuiGraphics gui) {
        State s = STATE.get();
        gui.flush();
        if (!s.legacyDepthTest) {
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        }
        if (s.blend) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.blendFunc(s.blendSrc, s.blendDst);
        }
    }

    public static boolean depthTest() {
        return STATE.get().depthTest;
    }

    /**
     * The depth test legacy code has asked for, as the real GL state immediate draws (blits) run with: a legacy screen starts
     * with it off as in 1.7.10, while batched legacy geometry keeps the depth-tested GUI types it always drew with.
     */
    public static boolean mcc$legacyDepthTest() {
        return STATE.get().legacyDepthTest;
    }

    public static void mcc$setLegacyDepthTest(boolean on) {
        STATE.get().legacyDepthTest = on;
    }

    public static void setDepthTest(boolean on) {
        State s = STATE.get();
        s.legacyDepthTest = on;
        s.depthTest = on;
    }

    public static void setBlendFunc(int src, int dst) {
        State s = STATE.get();
        if (src == s.blendSrc && dst == s.blendDst) return;
        // what a screen batched was drawn with the previous function (the function travels with the render type, see stated)
        if (s.blend && gui(s) != null) flushBatch(s);
        s.blendSrc = src;
        s.blendDst = dst;
        s.additive = dst == 1; // GL_ONE
        s.additiveAlpha = s.additive && src == 770; // GL_SRC_ALPHA
    }

    public static void setLighting(boolean on) {
        STATE.get().lighting = on;
    }

    /** Light set through OpenGlHelper.setLightmapTextureCoords / Tessellator.setBrightness (-1 = context light). */
    public static void setForcedLight(int packed) {
        STATE.get().forcedLight = packed;
    }

    public static int vertexLight() {
        return vertexLight(STATE.get());
    }

    static int vertexLight(State s) {
        if (!s.lightmap) return LightTexture.FULL_BRIGHT;
        if (s.forcedLight >= 0) return s.forcedLight;
        Frame f = s.frames.peek();
        return f == null ? LightTexture.FULL_BRIGHT : f.light;
    }

    /** Buffer for raw legacy geometry (Tessellator, glBegin), chosen from the emulated texture and blend state. */
    public static VertexConsumer geometryBuffer() {
        return geometryBuffer(STATE.get());
    }

    private static VertexConsumer geometryBuffer(State s) {
        boolean gui = gui(s) != null;
        ResourceLocation texture = s.texture;
        MultiBufferSource buffers = buffers(s);
        if (gui && s.additive && s.texture2d) return buffers.getBuffer(stated(s, LegacyRenderTypes.guiAdditive(texture)));
        // a screen drawing with GL lighting off (Thaumcraft's aspect icons): the lit entity shader dimmed it
        // and a flat screen quad without a normal of its own, lighting or not: GUI item lighting left on by drawing items (the
        // Thaumonomicon's frame after its research icons) made the lit shader draw it black
        if (gui && (!s.lighting || s.flatGuiQuad) && s.texture2d && s.depthTest) return buffers.getBuffer(stated(s, LegacyRenderTypes.guiUnlit(texture)));
        if (s.blend && s.additiveAlpha && s.texture2d) return buffers.getBuffer(stated(s, LegacyRenderTypes.additiveAlpha(texture, s.depthTest)));
        if (!s.depthTest && !s.additive) {
            if (!s.texture2d) return buffers.getBuffer(stated(s, LegacyRenderTypes.untexturedNoDepth()));
            return buffers.getBuffer(stated(s, s.blend ? LegacyRenderTypes.translucentNoDepth(texture) : LegacyRenderTypes.cutoutNoDepth(texture)));
        }
        if (!s.texture2d) return buffers.getBuffer(stated(s, s.additive ? RenderType.lightning() : RenderType.debugQuads()));
        if (s.additive) return buffers.getBuffer(stated(s, RenderType.eyes(texture)));
        return buffers.getBuffer(stated(s, s.blend ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture)));
    }

    // Depth function, depth writes and colour writes. Batched legacy geometry draws through modern render types, which set
    // their own (LEQUAL, both writes on) when the batch is drawn, so a screen that masks with the depth buffer lost the mask:
    // JourneyMap draws its minimap shape into depth only (colour writes off) and then the map with GL_GREATER, and the map
    // covered the screen around the minimap instead. In a screen the legacy values travel with the render type, and a
    // change or a depth clear first draws what was batched under the previous state.

    private record Stated(RenderType base, int depthFunc, boolean depthWrite, int colorWrite, int blendSrc, int blendDst) {}
    private static final java.util.Map<Stated, RenderType> STATED = new java.util.concurrent.ConcurrentHashMap<>();

    public static void setDepthFunc(int func) {
        State s = STATE.get();
        if (func == s.depthFunc) return;
        if (gui(s) != null) flushBatch(s);
        s.depthFunc = func;
    }

    public static void setDepthWrite(boolean on) {
        State s = STATE.get();
        if (on == s.depthWrite) return;
        if (gui(s) != null) flushBatch(s);
        s.depthWrite = on;
    }

    public static void setColorWrite(boolean r, boolean g, boolean b, boolean a) {
        State s = STATE.get();
        int bits = (r ? 1 : 0) | (g ? 2 : 0) | (b ? 4 : 0) | (a ? 8 : 0);
        if (bits == s.colorWrite) return;
        if (gui(s) != null) flushBatch(s);
        s.colorWrite = bits;
    }

    /** Before a legacy glClear: what a screen batched was drawn before the clear in 1.7.10. */
    public static void beforeClear() {
        State s = STATE.get();
        if (gui(s) != null && RenderSystem.isOnRenderThread()) flushBatch(s);
    }

    /**
     * The render type, drawing with the legacy depth function and write masks when a screen changed them, and with the legacy
     * blend function wherever blending is on with one other than alpha blending: the types only know alpha blending and
     * additive light, so DragonAPI's other modes drew as alpha blending. ChromatiCraft draws its glows, halos and rune icons
     * (Crystal Repeater and the other network tiles, in the world and as items in slots) with BlendMode.ADDITIVEDARK,
     * glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR), on textures with an opaque black background: black adds nothing there,
     * alpha blended it covered the slot and its neighbours with black squares and starbursts.
     */
    private static RenderType stated(State s, RenderType base) {
        boolean depthState = gui(s) != null && !(s.depthFunc == 515 && s.depthWrite && s.colorWrite == 0xF);
        boolean blendState = s.blend && (s.blendSrc != 770 || s.blendDst != 771);
        if (!depthState && !blendState) return base;
        // -1: the part is not carried, the base type's own state stays (a translucent type's depth writes are off)
        Stated key = new Stated(base, depthState ? s.depthFunc : -1, !depthState || s.depthWrite, depthState ? s.colorWrite : 0xF,
                blendState ? s.blendSrc : -1, blendState ? s.blendDst : -1);
        RenderType known = STATED.get(key);
        if (known != null) return known;
        return STATED.computeIfAbsent(key, k -> new RenderType(
                base + (k.depthFunc() < 0 ? "" : "/legacy_depth_" + k.depthFunc() + (k.depthWrite() ? "w" : "") + "_c" + k.colorWrite())
                        + (k.blendSrc() < 0 ? "" : "/legacy_blend_" + k.blendSrc() + "_" + k.blendDst()), base.format(), base.mode(),
                base.bufferSize(), base.affectsCrumbling(), base.sortOnUpload(), () -> {
                    base.setupRenderState();
                    if (k.depthFunc() >= 0) {
                        RenderSystem.depthFunc(k.depthFunc());
                        RenderSystem.depthMask(k.depthWrite());
                        RenderSystem.colorMask((k.colorWrite() & 1) != 0, (k.colorWrite() & 2) != 0, (k.colorWrite() & 4) != 0, (k.colorWrite() & 8) != 0);
                    }
                    if (k.blendSrc() >= 0) {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(k.blendSrc(), k.blendDst());
                    }
                }, () -> {
                    base.clearRenderState();
                    if (k.depthFunc() >= 0) {
                        RenderSystem.depthFunc(515);
                        RenderSystem.depthMask(true);
                        RenderSystem.colorMask(true, true, true, true);
                    }
                    if (k.blendSrc() >= 0) {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
                }) {});
    }

    /** Whether the current geometry buffer takes the full entity vertex format (uv, overlay, light, normal). */
    public static boolean geometryHasEntityFormat() {
        return STATE.get().texture2d;
    }

    /** Quads handed to quad() on this thread so far (diagnostics: did a legacy renderer draw anything). */
    public static long quadCount() {
        return STATE.get().quadCount;
    }

    /** Diagnostics: min x,y,z / max x,y,z of the quads this thread draws while non-null. */
    public static double[] bounds() {
        return STATE.get().bounds;
    }

    public static void setBounds(double[] bounds) {
        STATE.get().bounds = bounds;
    }

    /** Set while LegacyVertices emits a screen quad the legacy code gave no normal (see geometryBuffer). */
    static void setFlatGuiQuad(boolean on) {
        STATE.get().flatGuiQuad = on;
    }

    /**
     * Emits one quad of legacy geometry (positions in the current matrix space). Corners are x, y, z, u, v, argb, light (and
     * optionally the own-colour flag); normal is the face normal. The corner arrays are only read.
     */
    public static void quad(double[][] v, float nx, float ny, float nz) {
        quad(STATE.get(), v[0], v[1], v[2], v[3], nx, ny, nz);
    }

    static void quad(State s, double[] v0, double[] v1, double[] v2, double[] v3, float nx, float ny, float nz) {
        s.quadCount++;
        if (s.textureMatrix != null && s.texture2d) {
            v0 = textured(s, v0, 0);
            v1 = textured(s, v1, 1);
            v2 = textured(s, v2, 2);
            v3 = textured(s, v3, 3);
        }
        if (s.bounds != null) {
            bound(s.bounds, v0);
            bound(s.bounds, v1);
            bound(s.bounds, v2);
            bound(s.bounds, v3);
        }
        PoseStack p = pose(s);
        if (p == null) return;
        Capture capture = s.capture;
        if (capture != null) {
            org.joml.Matrix4f m = p.last().pose();
            int q = capture.add();
            // In double: captured positions are absolute block coordinates, made block-relative only when baked. Through a
            // float they were snapped to a float's step, 2 blocks at a Sable / Create Aeronautics plot (~20 million blocks
            // out): ChromatiCraft's crystals came out as skewed triangles and Thaumcraft's jar glass vanished on ships.
            int o = q * Capture.STRIDE * Capture.CORNERS;
            o = captureCorner(s, capture.corners, o, m, v0);
            o = captureCorner(s, capture.corners, o, m, v1);
            o = captureCorner(s, capture.corners, o, m, v2);
            captureCorner(s, capture.corners, o, m, v3);
            org.joml.Vector3f tmp = s.normal.set(nx, ny, nz);
            p.last().normal().transform(tmp);
            capture.normals[q * 3] = tmp.x;
            capture.normals[q * 3 + 1] = tmp.y;
            capture.normals[q * 3 + 2] = tmp.z;
            return;
        }
        // one buffer lookup per draw: the emulated state cannot change while a draw's quads are emitted
        VertexConsumer buffer = s.drawBuffer;
        if (buffer == null) {
            buffer = geometryBuffer(s);
            if (s.inDraw) s.drawBuffer = buffer;
        }
        PoseStack.Pose last = p.last();
        boolean entity = s.texture2d;
        int overlay = s.frames.peek().overlay;
        emitCorner(s, buffer, last, entity, overlay, v0, nx, ny, nz);
        emitCorner(s, buffer, last, entity, overlay, v1, nx, ny, nz);
        emitCorner(s, buffer, last, entity, overlay, v2, nx, ny, nz);
        emitCorner(s, buffer, last, entity, overlay, v3, nx, ny, nz);
    }

    private static void bound(double[] bounds, double[] p) {
        for (int i = 0; i < 3; i++) {
            bounds[i] = Math.min(bounds[i], p[i]);
            bounds[3 + i] = Math.max(bounds[3 + i], p[i]);
        }
    }

    private static int captureCorner(State s, double[] out, int o, org.joml.Matrix4f m, double[] c) {
        double x = c[0], y = c[1], z = c[2];
        out[o] = m.m00() * x + m.m10() * y + m.m20() * z + m.m30();
        out[o + 1] = m.m01() * x + m.m11() * y + m.m21() * z + m.m31();
        out[o + 2] = m.m02() * x + m.m12() * y + m.m22() * z + m.m32();
        int n = Math.min(c.length, Capture.STRIDE);
        for (int k = 3; k < n; k++) out[o + k] = c[k];
        for (int k = n; k < Capture.STRIDE; k++) out[o + k] = 0.0D;
        if (n > 5) out[o + 5] = color(s, c);
        return o + Capture.STRIDE;
    }

    /**
     * A corner's colour (ARGB): its own, or while a draw is emitted for a corner without one ({@code [7] == 0}) the colour current at
     * the draw (a display list replays its draws under the caller's glColor). The corner arrays are not changed, so a compiled list can
     * be called by several threads.
     */
    static long color(State s, double[] x) {
        return s.drawColorSet && x.length > 7 && x[7] == 0.0D ? s.drawColor : (long) x[5];
    }

    private static void emitCorner(State s, VertexConsumer buffer, PoseStack.Pose last, boolean entity, int overlay, double[] x, float nx, float ny, float nz) {
        int argb = (int) color(s, x);
        var vc = buffer.addVertex(last, (float) x[0], (float) x[1], (float) x[2]).setColor(argb);
        if (s.texture2d) vc.setUv((float) x[3], (float) x[4]);
        if (entity) {
            vc.setOverlay(overlay);
            vc.setLight(s.lightmap ? (int) x[6] : LightTexture.FULL_BRIGHT); // lightmap unit off: its brightness was not applied
            vc.setNormal(last, nx, ny, nz);
        }
    }

    public static void resetState() {
        State s = STATE.get();
        s.legacyDepthTest = true;
        s.texture2d = true;
        s.blend = false;
        s.blendSrc = 770;
        s.blendDst = 771;
        s.depthTest = true;
        s.depthFunc = 515;
        s.depthWrite = true;
        s.colorWrite = 0xF;
        s.additive = false;
        s.additiveAlpha = false;
        s.lighting = true;
        s.forcedLight = -1;
        s.activeUnit = 0;
        s.lightmap = true;
        s.attribs.clear();
    }

    // glPushAttrib / glPopAttrib over the emulated state. Without them a state change made between the two outlived the
    // pop: ChromatiCraft's tile renderers (drawn for its items in slots too) push all attributes, switch to
    // BlendMode.ADDITIVEDARK with depth writes off and pop, and the block drawn after them took the leftover blending.

    private record Attribs(int mask, boolean texture2d, boolean blend, int blendSrc, int blendDst, boolean legacyDepthTest,
            boolean depthTest, int depthFunc, boolean depthWrite, int colorWrite, boolean lighting, float r, float g, float b, float a,
            int activeUnit, boolean lightmap) {}

    private static final int GL_CURRENT_BIT = 0x1, GL_LIGHTING_BIT = 0x40, GL_DEPTH_BUFFER_BIT = 0x100, GL_ENABLE_BIT = 0x2000,
            GL_COLOR_BUFFER_BIT = 0x4000, GL_TEXTURE_BIT = 0x40000;

    public static void pushAttrib(int mask) {
        State s = STATE.get();
        if (s.attribs.size() >= 16) return; // GL's own limit (GL_STACK_OVERFLOW): a renderer that never pops must not grow it
        s.attribs.push(new Attribs(mask, s.texture2d, s.blend, s.blendSrc, s.blendDst, s.legacyDepthTest, s.depthTest, s.depthFunc, s.depthWrite,
                s.colorWrite, s.lighting, s.r, s.g, s.b, s.a, s.activeUnit, s.lightmap));
    }

    /** Puts the saved values back through the legacy GL calls, so the real GL state follows the emulated one. */
    public static void popAttrib() {
        State st = STATE.get();
        Attribs s = st.attribs.poll();
        if (s == null) return;
        int m = s.mask();
        int current = st.activeUnit;
        st.activeUnit = 0; // GL_TEXTURE_2D below is the main texture's
        if ((m & (GL_ENABLE_BIT | GL_TEXTURE_BIT)) != 0) {
            enable(3553, s.texture2d()); // GL_TEXTURE_2D
            st.lightmap = s.lightmap(); // DragonAPI's disableEntityLighting between push and pop must not leave everything full bright
        }
        if ((m & (GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT)) != 0) enable(3042, s.blend()); // GL_BLEND
        if ((m & (GL_ENABLE_BIT | GL_DEPTH_BUFFER_BIT)) != 0) {
            enable(2929, s.legacyDepthTest()); // GL_DEPTH_TEST
            st.depthTest = s.depthTest(); // a screen's batched geometry depth-tests while its blits do not (see legacyDepthTest)
        }
        if ((m & (GL_ENABLE_BIT | GL_LIGHTING_BIT)) != 0) enable(2896, s.lighting()); // GL_LIGHTING
        if ((m & GL_COLOR_BUFFER_BIT) != 0) {
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glBlendFunc(s.blendSrc(), s.blendDst());
            int c = s.colorWrite();
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glColorMask((c & 1) != 0, (c & 2) != 0, (c & 4) != 0, (c & 8) != 0);
        }
        if ((m & GL_DEPTH_BUFFER_BIT) != 0) {
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glDepthFunc(s.depthFunc());
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glDepthMask(s.depthWrite());
        }
        if ((m & GL_CURRENT_BIT) != 0) color(s.r(), s.g(), s.b(), s.a());
        st.activeUnit = (m & GL_TEXTURE_BIT) != 0 ? s.activeUnit() : current;
    }

    private static void enable(int cap, boolean on) {
        if (on) legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glEnable(cap);
        else legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glDisable(cap);
    }

    /**
     * A GL_LINES segment in the world through the line pipeline (screen-space width); false when there is nowhere to draw it
     * (chunk capture), and the caller draws a thin quad instead.
     */
    static boolean line(double[] a, double[] b) {
        State s = STATE.get();
        PoseStack p = pose(s);
        if (p == null || s.capture != null) return false;
        s.quadCount++;
        s.drawBuffer = null; // another render type: the quads' buffer may be ended by this lookup
        VertexConsumer buffer = buffers(s).getBuffer(LegacyRenderTypes.lines(s.depthTest));
        PoseStack.Pose last = p.last();
        float nx = (float) (b[0] - a[0]), ny = (float) (b[1] - a[1]), nz = (float) (b[2] - a[2]);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len;
        ny /= len;
        nz /= len;
        buffer.addVertex(last, (float) a[0], (float) a[1], (float) a[2]).setColor((int) color(s, a)).setNormal(last, nx, ny, nz);
        buffer.addVertex(last, (float) b[0], (float) b[1], (float) b[2]).setColor((int) color(s, b)).setNormal(last, nx, ny, nz);
        return true;
    }

    /** Entity-style buffer for the bound texture, used by legacy models. */
    public static VertexConsumer modelBuffer() {
        State s = STATE.get();
        // model boxes count as drawn geometry too (an inventory block handler drawing a TESR model: Thaumcraft's crystals)
        s.quadCount++;
        MultiBufferSource buffers = buffers(s);
        if (s.armorLayering) return buffers.getBuffer(RenderType.armorCutoutNoCull(s.texture));
        // blended models keep their blending, as raw geometry does: Thaumcraft's infusion matrix draws its glowing runes as
        // a second additive cube over the stone one, which drew opaque
        if (s.blend && s.texture2d && gui(s) == null) {
            if (s.additiveAlpha) return buffers.getBuffer(stated(s, LegacyRenderTypes.additiveAlpha(s.texture, s.depthTest)));
            if (s.additive) return buffers.getBuffer(stated(s, RenderType.eyes(s.texture)));
            return buffers.getBuffer(stated(s, RenderType.entityTranslucent(s.texture)));
        }
        return buffers.getBuffer(RenderType.entityCutoutNoCull(s.texture));
    }

    /**
     * Drawing legacy armor: models go in front of the body they lie on, as modern armor does (Fisk's Heroes' suits use the
     * body's own boxes and lost to the skin at the same depth).
     */
    public static void setArmorLayering(boolean on) {
        STATE.get().armorLayering = on;
    }

    // ------------------------------------------------------------------ matrix stack

    public static void push() {
        State s = STATE.get();
        if (s.matrixMode == GL_TEXTURE) {
            s.textureStack.push(s.textureMatrix == null ? new org.joml.Matrix4f() : new org.joml.Matrix4f(s.textureMatrix));
            return;
        }
        if (s.matrixMode == GL_PROJECTION) {
            if (beginProjection(s)) s.projectionStack.push(new org.joml.Matrix4f(s.projection));
            return;
        }
        PoseStack p = pose(s);
        if (p != null) p.pushPose();
        else s.strayPushes++;
    }

    public static void pop() {
        State s = STATE.get();
        if (s.matrixMode == GL_TEXTURE) {
            s.textureMatrix = s.textureStack.isEmpty() ? null : s.textureStack.pop();
            return;
        }
        if (s.matrixMode == GL_PROJECTION) {
            if (s.projection == null || s.projectionStack.isEmpty()) return;
            org.joml.Matrix4f previous = s.projectionStack.pop();
            if (s.projectionStack.isEmpty() && previous.equals(s.savedProjection)) endProjection(s);
            else setProjection(s, previous);
            return;
        }
        Frame f = s.frames.peek();
        PoseStack p = f == null ? null : f.pose;
        // A pop with nothing pushed (legacy code is sloppy) must not remove the caller's base matrix: the caller's own pushes are
        // below it, and once the base is gone end() pops the whole stack looking for it (an item drawn in FTB Quests then failed
        // with an empty PoseStack in ItemRenderer.render).
        if (p != null) {
            if (!p.clear() && p.last() != f.base) p.popPose();
        } else if (s.strayPushes > 0) {
            s.strayPushes--;
        }
    }

    public static void translate(double x, double y, double z) {
        State s = STATE.get();
        if (s.matrixMode != GL_MODELVIEW && multMatrix(s, new org.joml.Matrix4f().translation((float) x, (float) y, (float) z))) return;
        PoseStack p = pose(s);
        if (p != null) p.translate(x, y, z);
    }

    public static void rotate(float degrees, float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) return;
        State s = STATE.get();
        if (s.matrixMode != GL_MODELVIEW && multMatrix(s, new org.joml.Matrix4f().rotation((float) Math.toRadians(degrees), x / len, y / len, z / len))) return;
        PoseStack p = pose(s);
        if (p == null) return;
        p.mulPose(s.rotation.rotationAxis((float) Math.toRadians(degrees), x / len, y / len, z / len));
    }

    public static void scale(float x, float y, float z) {
        State s = STATE.get();
        if (s.matrixMode != GL_MODELVIEW && multMatrix(s, new org.joml.Matrix4f().scaling(x, y, z))) return;
        PoseStack p = pose(s);
        if (p != null) p.scale(x, y, z);
    }

    // ------------------------------------------------------------------ matrix modes (glMatrixMode)
    //
    // 1.7.10 code switches to GL_TEXTURE to scroll or scale texture coordinates (glints, energy fields) and to GL_PROJECTION
    // to draw its own perspective or orthographic view (Galacticraft's galaxy map, 3D previews in screens). Without the modes
    // those calls landed on the model-view matrix and moved the geometry instead.

    public static void matrixMode(int mode) {
        STATE.get().matrixMode = mode;
    }

    /** glLoadIdentity: the texture or legacy projection matrix, or the model-view while a legacy projection is in use. */
    public static void loadIdentity() {
        loadMatrix(new org.joml.Matrix4f());
    }

    public static void loadMatrix(org.joml.Matrix4f m) {
        State s = STATE.get();
        if (s.matrixMode == GL_TEXTURE) {
            s.textureMatrix = new org.joml.Matrix4f(m);
        } else if (s.matrixMode == GL_PROJECTION) {
            if (beginProjection(s)) setProjection(s, m);
        } else if (s.projection != null && pose(s) != null) {
            // the model-view: only once the legacy code has its own projection (1.7.10's identity then means "no camera");
            // under the modern projection an identity would drop the screen or world transform the legacy code never set up
            pose(s).last().pose().set(m);
            pose(s).last().normal().identity();
        }
    }

    /** glMultMatrix, glOrtho and glFrustum (and translate/rotate/scale) in the texture or projection mode; false in the model-view mode. */
    public static boolean multMatrix(org.joml.Matrix4f m) {
        return multMatrix(STATE.get(), m);
    }

    private static boolean multMatrix(State s, org.joml.Matrix4f m) {
        if (s.matrixMode == GL_TEXTURE) {
            if (s.textureMatrix == null) s.textureMatrix = new org.joml.Matrix4f();
            s.textureMatrix.mul(m);
            return true;
        }
        if (s.matrixMode == GL_PROJECTION) {
            if (beginProjection(s)) setProjection(s, new org.joml.Matrix4f(s.projection).mul(m));
            return true;
        }
        return false;
    }

    /**
     * Starts emulating the projection: the modern one is saved, and the modern base model-view (the screen's depth offset)
     * moves into the current pose so that it can be identity while legacy code projects with its own matrix. False without a
     * legacy frame or off the render thread (then the calls are ignored as before).
     */
    private static boolean beginProjection(State s) {
        if (s.projection != null) return true;
        PoseStack p = pose(s);
        if (p == null || !RenderSystem.isOnRenderThread()) return false;
        flushBatch(s);
        s.savedProjection = new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());
        s.savedSorting = RenderSystem.getVertexSorting();
        s.savedModelView = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix());
        s.projection = new org.joml.Matrix4f(s.savedProjection);
        s.foldedPose = p.last();
        s.foldedPose.pose().mulLocal(s.savedModelView);
        var stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.identity();
        RenderSystem.applyModelViewMatrix();
        return true;
    }

    private static void setProjection(State s, org.joml.Matrix4f m) {
        flushBatch(s);
        s.projection = new org.joml.Matrix4f(m);
        RenderSystem.setProjectionMatrix(s.projection, s.savedSorting);
    }

    /** Puts the modern projection and model-view back (the legacy code popped its projection, or the frame ended). */
    private static void endProjection(State s) {
        if (s.projection == null) return;
        flushBatch(s);
        RenderSystem.setProjectionMatrix(s.savedProjection, s.savedSorting);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        PoseStack p = pose(s);
        if (p != null && p.last() == s.foldedPose) s.foldedPose.pose().mulLocal(new org.joml.Matrix4f(s.savedModelView).invert());
        s.projection = null;
        s.projectionStack.clear();
        s.foldedPose = null;
    }

    /** Draws the geometry batched so far, before the projection it was meant for changes. */
    private static void flushBatch(State s) {
        GuiGraphics gui = gui(s);
        if (gui != null) {
            mcc$flushGui(gui);
        } else if (buffers(s) instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    /** Texture coordinates of a corner through the emulated texture matrix, into the scratch corner {@code slot}. */
    private static double[] textured(State s, double[] v, int slot) {
        double[] out = s.uvScratch[slot];
        if (out.length < v.length) out = s.uvScratch[slot] = new double[v.length];
        System.arraycopy(v, 0, out, 0, v.length);
        for (int k = v.length; k < out.length; k++) out[k] = 0.0D;
        s.textureMatrix.transform(s.uv.set((float) v[3], (float) v[4], 0F, 1F));
        out[3] = s.uv.x;
        out[4] = s.uv.y;
        return out;
    }

    private static void resetMatrixModes(State s) {
        endProjection(s);
        s.matrixMode = GL_MODELVIEW;
        s.textureMatrix = null;
        s.textureStack.clear();
    }
}
