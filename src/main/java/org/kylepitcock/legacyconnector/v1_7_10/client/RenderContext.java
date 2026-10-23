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

    private static final Deque<Frame> FRAMES = new ArrayDeque<>();
    /**
     * Legacy renderers share global state (the Tessellator, the emulated GL state, these frames), so every use is
     * serialized: the render thread holds the lock for the duration of a legacy render, and chunk-meshing threads for
     * the duration of a captured block render. Reentrant for nested frames on one thread.
     */
    private static final java.util.concurrent.locks.ReentrantLock LOCK = new java.util.concurrent.locks.ReentrantLock();
    private static Capture capture;

    /** Geometry drawn while capturing: quads of {x, y, z, u, v, argb, light} corners and their face normals. */
    public static final class Capture {
        public final java.util.List<double[][]> quads = new java.util.ArrayList<>();
        public final java.util.List<float[]> normals = new java.util.ArrayList<>();
    }
    private static ResourceLocation texture = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static float r = 1, g = 1, b = 1, a = 1;
    /** Matrices pushed by legacy code outside any context (should be rare): count only, so pops stay balanced. */
    private static int strayPushes;

    private RenderContext() {}

    public static void beginGui(GuiGraphics gui) {
        LOCK.lock();
        FRAMES.push(new Frame(gui, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, gui.pose().last()));
        resetColor();
    }

    public static void beginWorld(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        LOCK.lock();
        FRAMES.push(new Frame(null, pose, buffers, light, overlay, pose.last()));
        resetColor();
    }

    public static void end() {
        try {
            Frame f = FRAMES.pop();
            if (FRAMES.isEmpty()) resetState();
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
        FRAMES.push(new Frame(null, pose, null, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose.last()));
        resetColor();
        Capture c = new Capture();
        capture = c;
        return c;
    }

    public static void endCapture() {
        capture = null;
        end();
    }

    public static boolean active() {
        return !FRAMES.isEmpty();
    }

    public static GuiGraphics gui() {
        Frame f = FRAMES.peek();
        return f == null ? null : f.gui;
    }

    public static PoseStack pose() {
        Frame f = FRAMES.peek();
        return f == null ? null : f.pose;
    }

    public static MultiBufferSource buffers() {
        Frame f = FRAMES.peek();
        if (f != null) return f.buffers;
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    public static int light() {
        Frame f = FRAMES.peek();
        return f == null ? LightTexture.FULL_BRIGHT : f.light;
    }

    public static int overlay() {
        Frame f = FRAMES.peek();
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

    public static void color(float red, float green, float blue, float alpha) {
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
    private static boolean additive;
    private static boolean lighting = true;
    private static int forcedLight = -1;

    public static void setTexture2d(boolean on) {
        texture2d = on;
    }

    public static void setBlend(boolean on) {
        blend = on;
    }

    public static void setBlendFunc(int src, int dst) {
        additive = dst == 1; // GL_ONE
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
        texture2d = true;
        blend = false;
        additive = false;
        lighting = true;
        forcedLight = -1;
    }

    /** Entity-style buffer for the bound texture, used by legacy models. */
    public static VertexConsumer modelBuffer() {
        return buffers().getBuffer(RenderType.entityCutoutNoCull(texture));
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
