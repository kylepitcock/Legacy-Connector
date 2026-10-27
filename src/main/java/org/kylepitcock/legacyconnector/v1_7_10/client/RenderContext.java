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

    public static void setBlend(boolean on) {
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
        return forcedLight >= 0 ? forcedLight : light();
    }

    /** Buffer for raw legacy geometry (Tessellator, glBegin), chosen from the emulated texture and blend state. */
    public static VertexConsumer geometryBuffer() {
        if (gui() != null && additive && texture2d) return buffers().getBuffer(LegacyRenderTypes.guiAdditive(texture));
        // a screen drawing with GL lighting off (Thaumcraft's aspect icons): the lit entity shader dimmed it
        // and a flat screen quad without a normal of its own, lighting or not: GUI item lighting left on by drawing items (the
        // Thaumonomicon's frame after its research icons) made the lit shader draw it black
        if (gui() != null && (!lighting || flatGuiQuad) && texture2d && depthTest) return buffers().getBuffer(LegacyRenderTypes.guiUnlit(texture));
        if (blend && additiveAlpha && texture2d) return buffers().getBuffer(LegacyRenderTypes.additiveAlpha(texture, depthTest));
        if (!depthTest && !additive) {
            if (!texture2d) return buffers().getBuffer(LegacyRenderTypes.untexturedNoDepth());
            return buffers().getBuffer(blend ? LegacyRenderTypes.translucentNoDepth(texture) : LegacyRenderTypes.cutoutNoDepth(texture));
        }
        if (!texture2d) return buffers().getBuffer(additive ? RenderType.lightning() : RenderType.debugQuads());
        if (additive) return buffers().getBuffer(RenderType.eyes(texture));
        return buffers().getBuffer(blend ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture));
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
            org.joml.Vector3f tmp = new org.joml.Vector3f();
            for (int i = 0; i < 4; i++) {
                double[] c = v[i].clone();
                tmp.set((float) c[0], (float) c[1], (float) c[2]);
                m.transformPosition(tmp);
                c[0] = tmp.x;
                c[1] = tmp.y;
                c[2] = tmp.z;
                out[i] = c;
            }
            tmp.set(nx, ny, nz);
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
                vc.setLight((int) x[6]);
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
        additive = false;
        additiveAlpha = false;
        lighting = true;
        forcedLight = -1;
    }

    /** Entity-style buffer for the bound texture, used by legacy models. */
    public static VertexConsumer modelBuffer() {
        // model boxes count as drawn geometry too (an inventory block handler drawing a TESR model: Thaumcraft's crystals)
        quadCount++;
        return buffers().getBuffer(armorLayering ? RenderType.armorCutoutNoCull(texture) : RenderType.entityCutoutNoCull(texture));
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
        PoseStack p = pose();
        if (p != null) p.pushPose();
        else strayPushes++;
    }

    public static void pop() {
        PoseStack p = pose();
        // A pop with nothing pushed (legacy code is sloppy) must not remove the caller's base matrix.
        if (p != null) {
            if (!p.clear()) p.popPose();
        } else if (strayPushes > 0) {
            strayPushes--;
        }
    }

    public static void translate(double x, double y, double z) {
        PoseStack p = pose();
        if (p != null) p.translate(x, y, z);
    }

    public static void rotate(float degrees, float x, float y, float z) {
        PoseStack p = pose();
        if (p == null) return;
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) return;
        p.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(degrees), x / len, y / len, z / len));
    }

    public static void scale(float x, float y, float z) {
        PoseStack p = pose();
        if (p != null) p.scale(x, y, z);
    }
}
