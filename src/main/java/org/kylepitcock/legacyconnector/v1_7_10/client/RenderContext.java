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
 * <p>Render-thread only; contexts nest (a TESR may render an item that uses another renderer).</p>
 */
public final class RenderContext {
    private record Frame(GuiGraphics gui, PoseStack pose, MultiBufferSource buffers, int light, int overlay, PoseStack.Pose base) {}

    /** Per thread: chunk-meshing threads capture blocks while the render thread draws, and must not see each other's frames. */
    private static final ThreadLocal<Deque<Frame>> FRAME_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    /**
     * Legacy renderers share global state (the Tessellator, the emulated GL state, these frames), so every use is
     * serialized: the render thread holds the lock for the duration of a legacy render, and chunk-meshing threads for
     * the duration of a captured block render. Reentrant for nested frames on one thread.
     */
    private static final java.util.concurrent.locks.ReentrantLock LOCK = new java.util.concurrent.locks.ReentrantLock();
    private static final ThreadLocal<Capture> CAPTURE = new ThreadLocal<>();

    /** Geometry drawn while capturing: quads of {x, y, z, u, v, argb, light} corners and their face normals. */
    public static final class Capture {
        public final java.util.List<double[][]> quads = new java.util.ArrayList<>();
        public final java.util.List<float[]> normals = new java.util.ArrayList<>();
    }
    private static ResourceLocation texture = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static float r = 1, g = 1, b = 1, a = 1;
    /** Matrices pushed by legacy code outside any context (should be rare): count only, so pops stay balanced. */
    private static int strayPushes;

    private static Deque<Frame> frames() {
        return FRAME_STACK.get();
    }

    private RenderContext() {}

    public static void beginGui(GuiGraphics gui) {
        LOCK.lock();
        if (frames().isEmpty()) legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.instance.mcc$reset();
        frames().push(new Frame(gui, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, gui.pose().last()));
        resetColor();
    }

    public static void beginWorld(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        LOCK.lock();
        if (frames().isEmpty()) legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.instance.mcc$reset();
        frames().push(new Frame(null, pose, buffers, light, overlay, pose.last()));
        resetColor();
    }

    public static void end() {
        try {
            Frame f = frames().pop();
            if (frames().isEmpty()) {
                // a legacy projection left in place must not outlive the frame (restored while the frame's target is current)
                frames().push(f);
                try {
                    resetMatrixModes();
                } finally {
                    frames().pop();
                }
                legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.instance.mcc$reset();
                resetState();
                // a depth function legacy code left set must not leak into modern rendering
                if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            }
            // Legacy code that pushed more matrices than it popped (or threw midway) must not corrupt the caller's stack.
            // (a legacy renderer that popped too much can leave the stack empty: last() would throw, so check clear() first)
            for (int guard = 0; !f.pose.clear() && f.pose.last() != f.base && guard < 64; guard++) f.pose.popPose();
            if (f.gui != null) f.gui.setColor(1, 1, 1, 1);
            resetColor();
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Starts capturing geometry instead of drawing it (a block rendered into a baked model). Positions come out in
     * the space of the emulated matrix stack, which starts as the identity.
     */
    public static Capture beginCapture() {
        LOCK.lock();
        PoseStack pose = new PoseStack();
        frames().push(new Frame(null, pose, null, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose.last()));
        resetColor();
        Capture c = new Capture();
        CAPTURE.set(c);
        return c;
    }

    public static void endCapture() {
        CAPTURE.remove();
        end();
    }

    public static boolean active() {
        return !frames().isEmpty();
    }

    public static GuiGraphics gui() {
        Frame f = frames().peek();
        return f == null ? null : f.gui;
    }

    public static PoseStack pose() {
        Frame f = frames().peek();
        return f == null ? null : f.pose;
    }

    public static MultiBufferSource buffers() {
        Frame f = frames().peek();
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
        checkExists(rl);
        texture = rl;
        RenderSystem.setShaderTexture(0, rl);
    }

    public static ResourceLocation texture() {
        return texture;
    }

    /** glColor4f: components are clamped to [0, 1] as GL did (Thaumcraft's crystals pass 255 / 220; unclamped they wrapped dark). */
    public static void color(float red, float green, float blue, float alpha) {
        red = Math.max(0.0F, Math.min(1.0F, red));
        green = Math.max(0.0F, Math.min(1.0F, green));
        blue = Math.max(0.0F, Math.min(1.0F, blue));
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        r = red;
        g = green;
        b = blue;
        a = alpha;
        GuiGraphics gui = gui();
        if (gui != null) gui.setColor(red, green, blue, alpha);
    }

    private static void resetColor() {
        r = g = b = a = 1;
    }

    public static int argb() {
        return ((int) (a * 255) & 255) << 24 | ((int) (r * 255) & 255) << 16 | ((int) (g * 255) & 255) << 8 | ((int) (b * 255) & 255);
    }

    // ------------------------------------------------------------------ emulated GL state and raw geometry

    private static boolean texture2d = true;
    private static boolean blend;
    private static boolean depthTest = true;
    private static boolean additive;
    /** Additive with the source weighted by alpha (GL_SRC_ALPHA, GL_ONE) rather than GL_ONE, GL_ONE. */
    private static boolean additiveAlpha;
    private static boolean lighting = true;
    private static int forcedLight = -1;

    public static void setTexture2d(boolean on) {
        texture2d = on;
    }

    // Texture units. 1.7.10 drew with two: GL_TEXTURE0 (the bound texture) and GL_TEXTURE1 (the lightmap), and legacy code
    // selects one with OpenGlHelper.setActiveTexture / GL13.glActiveTexture before switching GL_TEXTURE_2D or binding. The
    // vanilla idiom (EntityRenderer.disableLightmap, DragonAPI's ReikaRenderHelper.disableEntityLighting) is "lightmap unit,
    // glDisable(GL_TEXTURE_2D), default unit": full-bright drawing. Without units it switched texturing off on the main
    // texture, and ChromatiCraft's Crystal Pylon halo (atlas sprite, additive) drew as an untextured opaque white square.

    /** The emulated active texture unit, relative to GL_TEXTURE0. */
    private static int activeUnit;
    /** GL_TEXTURE_2D on the lightmap unit: off, the lightmap was not applied and legacy geometry drew full bright. */
    private static boolean lightmap = true;

    /** glActiveTexture(GL_TEXTURE0 + n): later GL_TEXTURE_2D switches and binds apply to that unit. */
    public static void setActiveTexture(int unit) {
        activeUnit = Math.max(0, Math.min(31, unit - 33984)); // GL_TEXTURE0
    }

    /** Whether the active unit is the main texture's (binds and GL_TEXTURE_2D act on the drawn texture). */
    public static boolean onDefaultTextureUnit() {
        return activeUnit == 0;
    }

    /** GL_TEXTURE_2D switched on the active unit: the main texture on unit 0, the lightmap on unit 1, ignored elsewhere. */
    public static void setUnitTexture2d(boolean on) {
        if (activeUnit == 0) texture2d = on;
        else if (activeUnit == 1) lightmap = on;
    }

    /** glIsEnabled(GL_TEXTURE_2D) for the active unit. */
    public static boolean unitTexture2d() {
        return activeUnit == 0 ? texture2d : activeUnit == 1 && lightmap;
    }

    public static void setBlend(boolean on) {
        if (on == blend) return;
        if (gui() != null && (blendSrc != 770 || blendDst != 771)) flushBatch();
        blend = on;
    }

    /**
     * Flushes a screen's batched geometry and puts the emulated depth test back: modern render types switch depth testing
     * on while they draw, and a legacy screen that runs with it off (1.7.10's default) must stay that way. Blending is put
     * back too: the render types switch it off when done, and a blit after them lost its alpha (Thaumonomicon tab shadows
     * drew solid black).
     */
    public static void mcc$flushGui(GuiGraphics gui) {
        gui.flush();
        if (!legacyDepthTest) {
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        }
        if (blend) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.blendFunc(blendSrc, blendDst);
        }
    }

    public static boolean depthTest() {
        return depthTest;
    }

    /**
     * The depth test legacy code has asked for, as the real GL state immediate draws (blits) run with: a legacy screen starts
     * with it off as in 1.7.10, while batched legacy geometry keeps the depth-tested GUI types it always drew with.
     */
    private static boolean legacyDepthTest = true;

    public static boolean mcc$legacyDepthTest() {
        return legacyDepthTest;
    }

    public static void mcc$setLegacyDepthTest(boolean on) {
        legacyDepthTest = on;
    }

    public static void setDepthTest(boolean on) {
        legacyDepthTest = on;
        depthTest = on;
    }

    private static int blendSrc = 770, blendDst = 771; // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA

    public static void setBlendFunc(int src, int dst) {
        if (src == blendSrc && dst == blendDst) return;
        // what a screen batched was drawn with the previous function (the function travels with the render type, see stated)
        if (blend && gui() != null) flushBatch();
        blendSrc = src;
        blendDst = dst;
        additive = dst == 1; // GL_ONE
        additiveAlpha = additive && src == 770; // GL_SRC_ALPHA
    }

    public static void setLighting(boolean on) {
        lighting = on;
    }

    /** Light set through OpenGlHelper.setLightmapTextureCoords / Tessellator.setBrightness (-1 = context light). */
    public static void setForcedLight(int packed) {
        forcedLight = packed;
    }

    public static int vertexLight() {
        if (!lightmap) return LightTexture.FULL_BRIGHT;
        return forcedLight >= 0 ? forcedLight : light();
    }

    /** Buffer for raw legacy geometry (Tessellator, glBegin), chosen from the emulated texture and blend state. */
    public static VertexConsumer geometryBuffer() {
        if (gui() != null && additive && texture2d) return buffers().getBuffer(stated(LegacyRenderTypes.guiAdditive(texture)));
        // a screen drawing with GL lighting off (Thaumcraft's aspect icons): the lit entity shader dimmed it
        // and a flat screen quad without a normal of its own, lighting or not: GUI item lighting left on by drawing items (the
        // Thaumonomicon's frame after its research icons) made the lit shader draw it black
        if (gui() != null && (!lighting || flatGuiQuad) && texture2d && depthTest) return buffers().getBuffer(stated(LegacyRenderTypes.guiUnlit(texture)));
        if (blend && additiveAlpha && texture2d) return buffers().getBuffer(stated(LegacyRenderTypes.additiveAlpha(texture, depthTest)));
        if (!depthTest && !additive) {
            if (!texture2d) return buffers().getBuffer(stated(LegacyRenderTypes.untexturedNoDepth()));
            return buffers().getBuffer(stated(blend ? LegacyRenderTypes.translucentNoDepth(texture) : LegacyRenderTypes.cutoutNoDepth(texture)));
        }
        if (!texture2d) return buffers().getBuffer(stated(additive ? RenderType.lightning() : RenderType.debugQuads()));
        if (additive) return buffers().getBuffer(stated(RenderType.eyes(texture)));
        return buffers().getBuffer(stated(blend ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture)));
    }

    // Depth function, depth writes and colour writes. Batched legacy geometry draws through modern render types, which set
    // their own (LEQUAL, both writes on) when the batch is drawn, so a screen that masks with the depth buffer lost the mask:
    // JourneyMap draws its minimap shape into depth only (colour writes off) and then the map with GL_GREATER, and the map
    // covered the screen around the minimap instead. In a screen the legacy values travel with the render type, and a
    // change or a depth clear first draws what was batched under the previous state.

    private static int depthFunc = 515; // GL_LEQUAL
    private static boolean depthWrite = true;
    private static int colorWrite = 0xF; // r, g, b, a bits
    private record Stated(RenderType base, int depthFunc, boolean depthWrite, int colorWrite, int blendSrc, int blendDst) {}
    private static final java.util.Map<Stated, RenderType> STATED = new java.util.concurrent.ConcurrentHashMap<>();

    public static void setDepthFunc(int func) {
        if (func == depthFunc) return;
        if (gui() != null) flushBatch();
        depthFunc = func;
    }

    public static void setDepthWrite(boolean on) {
        if (on == depthWrite) return;
        if (gui() != null) flushBatch();
        depthWrite = on;
    }

    public static void setColorWrite(boolean r, boolean g, boolean b, boolean a) {
        int bits = (r ? 1 : 0) | (g ? 2 : 0) | (b ? 4 : 0) | (a ? 8 : 0);
        if (bits == colorWrite) return;
        if (gui() != null) flushBatch();
        colorWrite = bits;
    }

    /** Before a legacy glClear: what a screen batched was drawn before the clear in 1.7.10. */
    public static void beforeClear() {
        if (gui() != null && RenderSystem.isOnRenderThread()) flushBatch();
    }

    /**
     * The render type, drawing with the legacy depth function and write masks when a screen changed them, and with the legacy
     * blend function wherever blending is on with one other than alpha blending: the types only know alpha blending and
     * additive light, so DragonAPI's other modes drew as alpha blending. ChromatiCraft draws its glows, halos and rune icons
     * (Crystal Repeater and the other network tiles, in the world and as items in slots) with BlendMode.ADDITIVEDARK,
     * glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR), on textures with an opaque black background: black adds nothing there,
     * alpha blended it covered the slot and its neighbours with black squares and starbursts.
     */
    private static RenderType stated(RenderType base) {
        boolean depthState = gui() != null && !(depthFunc == 515 && depthWrite && colorWrite == 0xF);
        boolean blendState = blend && (blendSrc != 770 || blendDst != 771);
        if (!depthState && !blendState) return base;
        // -1: the part is not carried, the base type's own state stays (a translucent type's depth writes are off)
        Stated key = new Stated(base, depthState ? depthFunc : -1, !depthState || depthWrite, depthState ? colorWrite : 0xF,
                blendState ? blendSrc : -1, blendState ? blendDst : -1);
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
        return texture2d;
    }

    /**
     * Emits one quad of legacy geometry (positions in the current matrix space). Vertices are x, y, z, u, v, argb,
     * light; normal is the face normal.
     */
    /** Quads handed to quad() so far (diagnostics: did a legacy renderer draw anything). */
    public static long quadCount;

    /** Diagnostics: min x,y,z / max x,y,z of the quads drawn while non-null. */
    public static double[] bounds;

    /** Set while LegacyVertices emits a screen quad the legacy code gave no normal (see geometryBuffer). */
    static boolean flatGuiQuad;

    public static void quad(double[][] v, float nx, float ny, float nz) {
        quadCount++;
        v = textured(v);
        if (bounds != null) {
            for (double[] p : v) {
                for (int i = 0; i < 3; i++) {
                    bounds[i] = Math.min(bounds[i], p[i]);
                    bounds[3 + i] = Math.max(bounds[3 + i], p[i]);
                }
            }
        }
        PoseStack p = pose();
        if (p == null) return;
        Capture capture = CAPTURE.get();
        if (capture != null) {
            org.joml.Matrix4f m = p.last().pose();
            org.joml.Matrix3f n = p.last().normal();
            double[][] out = new double[4][];
            // In double: captured positions are absolute block coordinates, made block-relative only when baked. Through a
            // float they were snapped to a float's step, 2 blocks at a Sable / Create Aeronautics plot (~20 million blocks
            // out): ChromatiCraft's crystals came out as skewed triangles and Thaumcraft's jar glass vanished on ships.
            for (int i = 0; i < 4; i++) {
                double[] c = v[i].clone();
                double x = c[0], y = c[1], z = c[2];
                c[0] = m.m00() * x + m.m10() * y + m.m20() * z + m.m30();
                c[1] = m.m01() * x + m.m11() * y + m.m21() * z + m.m31();
                c[2] = m.m02() * x + m.m12() * y + m.m22() * z + m.m32();
                out[i] = c;
            }
            org.joml.Vector3f tmp = new org.joml.Vector3f(nx, ny, nz);
            n.transform(tmp);
            capture.quads.add(out);
            capture.normals.add(new float[] {tmp.x, tmp.y, tmp.z});
            return;
        }
        VertexConsumer buffer = geometryBuffer();
        PoseStack.Pose last = p.last();
        boolean entity = geometryHasEntityFormat();
        for (double[] x : v) {
            int argb = (int) (long) x[5];
            var vc = buffer.addVertex(last, (float) x[0], (float) x[1], (float) x[2]).setColor(argb);
            if (texture2d) vc.setUv((float) x[3], (float) x[4]);
            if (entity) {
                vc.setOverlay(overlay());
                vc.setLight(lightmap ? (int) x[6] : LightTexture.FULL_BRIGHT); // lightmap unit off: its brightness was not applied
                vc.setNormal(last, nx, ny, nz);
            }
        }
    }

    public static void resetState() {
        legacyDepthTest = true;
        texture2d = true;
        blend = false;
        blendSrc = 770;
        blendDst = 771;
        depthTest = true;
        depthFunc = 515;
        depthWrite = true;
        colorWrite = 0xF;
        additive = false;
        additiveAlpha = false;
        lighting = true;
        forcedLight = -1;
        activeUnit = 0;
        lightmap = true;
        attribs.clear();
    }

    // glPushAttrib / glPopAttrib over the emulated state. Without them a state change made between the two outlived the
    // pop: ChromatiCraft's tile renderers (drawn for its items in slots too) push all attributes, switch to
    // BlendMode.ADDITIVEDARK with depth writes off and pop, and the block drawn after them took the leftover blending.

    private record Attribs(int mask, boolean texture2d, boolean blend, int blendSrc, int blendDst, boolean legacyDepthTest,
            boolean depthTest, int depthFunc, boolean depthWrite, int colorWrite, boolean lighting, float r, float g, float b, float a,
            int activeUnit, boolean lightmap) {}

    private static final java.util.ArrayDeque<Attribs> attribs = new java.util.ArrayDeque<>();

    private static final int GL_CURRENT_BIT = 0x1, GL_LIGHTING_BIT = 0x40, GL_DEPTH_BUFFER_BIT = 0x100, GL_ENABLE_BIT = 0x2000,
            GL_COLOR_BUFFER_BIT = 0x4000, GL_TEXTURE_BIT = 0x40000;

    public static void pushAttrib(int mask) {
        if (attribs.size() >= 16) return; // GL's own limit (GL_STACK_OVERFLOW): a renderer that never pops must not grow it
        attribs.push(new Attribs(mask, texture2d, blend, blendSrc, blendDst, legacyDepthTest, depthTest, depthFunc, depthWrite,
                colorWrite, lighting, r, g, b, a, activeUnit, lightmap));
    }

    /** Puts the saved values back through the legacy GL calls, so the real GL state follows the emulated one. */
    public static void popAttrib() {
        Attribs s = attribs.poll();
        if (s == null) return;
        int m = s.mask();
        int current = activeUnit;
        activeUnit = 0; // GL_TEXTURE_2D below is the main texture's
        if ((m & (GL_ENABLE_BIT | GL_TEXTURE_BIT)) != 0) {
            enable(3553, s.texture2d()); // GL_TEXTURE_2D
            lightmap = s.lightmap(); // DragonAPI's disableEntityLighting between push and pop must not leave everything full bright
        }
        if ((m & (GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT)) != 0) enable(3042, s.blend()); // GL_BLEND
        if ((m & (GL_ENABLE_BIT | GL_DEPTH_BUFFER_BIT)) != 0) {
            enable(2929, s.legacyDepthTest()); // GL_DEPTH_TEST
            depthTest = s.depthTest(); // a screen's batched geometry depth-tests while its blits do not (see legacyDepthTest)
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
        activeUnit = (m & GL_TEXTURE_BIT) != 0 ? s.activeUnit() : current;
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
        PoseStack p = pose();
        if (p == null || CAPTURE.get() != null) return false;
        quadCount++;
        VertexConsumer buffer = buffers().getBuffer(LegacyRenderTypes.lines(depthTest));
        PoseStack.Pose last = p.last();
        float nx = (float) (b[0] - a[0]), ny = (float) (b[1] - a[1]), nz = (float) (b[2] - a[2]);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len;
        ny /= len;
        nz /= len;
        for (double[] v : new double[][] {a, b}) {
            buffer.addVertex(last, (float) v[0], (float) v[1], (float) v[2]).setColor((int) (long) v[5]).setNormal(last, nx, ny, nz);
        }
        return true;
    }

    /** Entity-style buffer for the bound texture, used by legacy models. */
    public static VertexConsumer modelBuffer() {
        // model boxes count as drawn geometry too (an inventory block handler drawing a TESR model: Thaumcraft's crystals)
        quadCount++;
        if (armorLayering) return buffers().getBuffer(RenderType.armorCutoutNoCull(texture));
        // blended models keep their blending, as raw geometry does: Thaumcraft's infusion matrix draws its glowing runes as
        // a second additive cube over the stone one, which drew opaque
        if (blend && texture2d && gui() == null) {
            if (additiveAlpha) return buffers().getBuffer(stated(LegacyRenderTypes.additiveAlpha(texture, depthTest)));
            if (additive) return buffers().getBuffer(stated(RenderType.eyes(texture)));
            return buffers().getBuffer(stated(RenderType.entityTranslucent(texture)));
        }
        return buffers().getBuffer(RenderType.entityCutoutNoCull(texture));
    }

    /**
     * Drawing legacy armor: models go in front of the body they lie on, as modern armor does (Fisk's Heroes' suits use the
     * body's own boxes and lost to the skin at the same depth).
     */
    private static boolean armorLayering;

    public static void setArmorLayering(boolean on) {
        armorLayering = on;
    }

    // ------------------------------------------------------------------ matrix stack

    public static void push() {
        if (matrixMode == GL_TEXTURE) {
            textureStack.push(textureMatrix == null ? new org.joml.Matrix4f() : new org.joml.Matrix4f(textureMatrix));
            return;
        }
        if (matrixMode == GL_PROJECTION) {
            if (beginProjection()) projectionStack.push(new org.joml.Matrix4f(projection));
            return;
        }
        PoseStack p = pose();
        if (p != null) p.pushPose();
        else strayPushes++;
    }

    public static void pop() {
        if (matrixMode == GL_TEXTURE) {
            textureMatrix = textureStack.isEmpty() ? null : textureStack.pop();
            return;
        }
        if (matrixMode == GL_PROJECTION) {
            if (projection == null || projectionStack.isEmpty()) return;
            org.joml.Matrix4f previous = projectionStack.pop();
            if (projectionStack.isEmpty() && previous.equals(savedProjection)) endProjection();
            else setProjection(previous);
            return;
        }
        PoseStack p = pose();
        // A pop with nothing pushed (legacy code is sloppy) must not remove the caller's base matrix.
        if (p != null) {
            if (!p.clear()) p.popPose();
        } else if (strayPushes > 0) {
            strayPushes--;
        }
    }

    public static void translate(double x, double y, double z) {
        if (multMatrix(new org.joml.Matrix4f().translation((float) x, (float) y, (float) z))) return;
        PoseStack p = pose();
        if (p != null) p.translate(x, y, z);
    }

    public static void rotate(float degrees, float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) return;
        if (matrixMode != GL_MODELVIEW && multMatrix(new org.joml.Matrix4f().rotation((float) Math.toRadians(degrees), x / len, y / len, z / len))) return;
        PoseStack p = pose();
        if (p == null) return;
        p.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(degrees), x / len, y / len, z / len));
    }

    public static void scale(float x, float y, float z) {
        if (matrixMode != GL_MODELVIEW && multMatrix(new org.joml.Matrix4f().scaling(x, y, z))) return;
        PoseStack p = pose();
        if (p != null) p.scale(x, y, z);
    }

    // ------------------------------------------------------------------ matrix modes (glMatrixMode)
    //
    // 1.7.10 code switches to GL_TEXTURE to scroll or scale texture coordinates (glints, energy fields) and to GL_PROJECTION
    // to draw its own perspective or orthographic view (Galacticraft's galaxy map, 3D previews in screens). Without the modes
    // those calls landed on the model-view matrix and moved the geometry instead.

    private static final int GL_MODELVIEW = 5888, GL_PROJECTION = 5889, GL_TEXTURE = 5890;
    private static int matrixMode = GL_MODELVIEW;
    /** The emulated texture matrix applied to legacy texture coordinates; null is the identity. */
    private static org.joml.Matrix4f textureMatrix;
    private static final Deque<org.joml.Matrix4f> textureStack = new ArrayDeque<>();
    /** The legacy projection while legacy code has replaced the modern one; null while it has not. */
    private static org.joml.Matrix4f projection;
    private static final Deque<org.joml.Matrix4f> projectionStack = new ArrayDeque<>();
    private static org.joml.Matrix4f savedProjection;
    private static com.mojang.blaze3d.vertex.VertexSorting savedSorting;
    private static org.joml.Matrix4f savedModelView;
    private static PoseStack.Pose foldedPose;

    public static void matrixMode(int mode) {
        matrixMode = mode;
    }

    /** glLoadIdentity: the texture or legacy projection matrix, or the model-view while a legacy projection is in use. */
    public static void loadIdentity() {
        loadMatrix(new org.joml.Matrix4f());
    }

    public static void loadMatrix(org.joml.Matrix4f m) {
        if (matrixMode == GL_TEXTURE) {
            textureMatrix = new org.joml.Matrix4f(m);
        } else if (matrixMode == GL_PROJECTION) {
            if (beginProjection()) setProjection(m);
        } else if (projection != null && pose() != null) {
            // the model-view: only once the legacy code has its own projection (1.7.10's identity then means "no camera");
            // under the modern projection an identity would drop the screen or world transform the legacy code never set up
            pose().last().pose().set(m);
            pose().last().normal().identity();
        }
    }

    /** glMultMatrix, glOrtho and glFrustum (and translate/rotate/scale) in the texture or projection mode; false in the model-view mode. */
    public static boolean multMatrix(org.joml.Matrix4f m) {
        if (matrixMode == GL_TEXTURE) {
            if (textureMatrix == null) textureMatrix = new org.joml.Matrix4f();
            textureMatrix.mul(m);
            return true;
        }
        if (matrixMode == GL_PROJECTION) {
            if (beginProjection()) setProjection(new org.joml.Matrix4f(projection).mul(m));
            return true;
        }
        return false;
    }

    /**
     * Starts emulating the projection: the modern one is saved, and the modern base model-view (the screen's depth offset)
     * moves into the current pose so that it can be identity while legacy code projects with its own matrix. False without a
     * legacy frame or off the render thread (then the calls are ignored as before).
     */
    private static boolean beginProjection() {
        if (projection != null) return true;
        PoseStack p = pose();
        if (p == null || !RenderSystem.isOnRenderThread()) return false;
        flushBatch();
        savedProjection = new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());
        savedSorting = RenderSystem.getVertexSorting();
        savedModelView = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix());
        projection = new org.joml.Matrix4f(savedProjection);
        foldedPose = p.last();
        foldedPose.pose().mulLocal(savedModelView);
        var stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.identity();
        RenderSystem.applyModelViewMatrix();
        return true;
    }

    private static void setProjection(org.joml.Matrix4f m) {
        flushBatch();
        projection = new org.joml.Matrix4f(m);
        RenderSystem.setProjectionMatrix(projection, savedSorting);
    }

    /** Puts the modern projection and model-view back (the legacy code popped its projection, or the frame ended). */
    private static void endProjection() {
        if (projection == null) return;
        flushBatch();
        RenderSystem.setProjectionMatrix(savedProjection, savedSorting);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        PoseStack p = pose();
        if (p != null && p.last() == foldedPose) foldedPose.pose().mulLocal(new org.joml.Matrix4f(savedModelView).invert());
        projection = null;
        projectionStack.clear();
        foldedPose = null;
    }

    /** Draws the geometry batched so far, before the projection it was meant for changes. */
    private static void flushBatch() {
        GuiGraphics gui = gui();
        if (gui != null) {
            mcc$flushGui(gui);
        } else if (buffers() instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    /** Texture coordinates through the emulated texture matrix (a copy of the quad when it is not the identity). */
    private static double[][] textured(double[][] v) {
        if (textureMatrix == null || !texture2d) return v;
        double[][] out = new double[v.length][];
        org.joml.Vector4f uv = new org.joml.Vector4f();
        for (int i = 0; i < v.length; i++) {
            out[i] = v[i].clone();
            textureMatrix.transform(uv.set((float) out[i][3], (float) out[i][4], 0F, 1F));
            out[i][3] = uv.x;
            out[i][4] = uv.y;
        }
        return out;
    }

    private static void resetMatrixModes() {
        endProjection();
        matrixMode = GL_MODELVIEW;
        textureMatrix = null;
        textureStack.clear();
    }
}
