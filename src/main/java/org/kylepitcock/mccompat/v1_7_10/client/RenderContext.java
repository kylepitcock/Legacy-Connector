package org.kylepitcock.mccompat.v1_7_10.client;

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
    private static ResourceLocation texture = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static float r = 1, g = 1, b = 1, a = 1;
    /** Matrices pushed by legacy code outside any context (should be rare): count only, so pops stay balanced. */
    private static int strayPushes;

    private RenderContext() {}

    public static void beginGui(GuiGraphics gui) {
        FRAMES.push(new Frame(gui, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, gui.pose().last()));
        resetColor();
    }

    public static void beginWorld(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FRAMES.push(new Frame(null, pose, buffers, light, overlay, pose.last()));
        resetColor();
    }

    public static void end() {
        Frame f = FRAMES.pop();
        // Legacy code that pushed more matrices than it popped (or threw midway) must not corrupt the caller's stack.
        for (int guard = 0; f.pose.last() != f.base && !f.pose.clear() && guard < 64; guard++) f.pose.popPose();
        if (f.gui != null) f.gui.setColor(1, 1, 1, 1);
        resetColor();
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

    public static void bindTexture(ResourceLocation rl) {
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
        if (p != null) p.popPose();
        else if (strayPushes > 0) strayPushes--;
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
