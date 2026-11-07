package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/** Client-side signature changes (kept apart from {@link MiscCompat}: this class links client classes). */
public final class ClientCompat {
    private ClientCompat() {}

    /** {@code Minecraft.getFrameTime()}: the partial tick of this frame. */
    public static float getFrameTime(Minecraft mc) {
        return mc.getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static float getDeltaFrameTime(Minecraft mc) {
        return mc.getTimer().getRealtimeDeltaTicks();
    }

    public static float getPartialTick(Minecraft mc) {
        return mc.getTimer().getGameTimeDeltaPartialTick(false);
    }

    /** 1.20.1 color arguments (floats) to the packed ARGB int of 1.21.1. */
    public static int packColor(float r, float g, float b, float a) {
        return net.minecraft.util.FastColor.ARGB32.colorFromFloat(a, r, g, b);
    }

    public static float colorR(int argb) {
        return net.minecraft.util.FastColor.ARGB32.red(argb) / 255.0F;
    }

    public static float colorG(int argb) {
        return net.minecraft.util.FastColor.ARGB32.green(argb) / 255.0F;
    }

    public static float colorB(int argb) {
        return net.minecraft.util.FastColor.ARGB32.blue(argb) / 255.0F;
    }

    public static float colorA(int argb) {
        return net.minecraft.util.FastColor.ARGB32.alpha(argb) / 255.0F;
    }

    public static void renderPart(net.minecraft.client.model.geom.ModelPart part, PoseStack ps, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay,
                                  float r, float g, float b, float a) {
        part.render(ps, vc, light, overlay, packColor(r, g, b, a));
    }

    public static void renderToBuffer(net.minecraft.client.model.Model model, PoseStack ps, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay,
                                      float r, float g, float b, float a) {
        model.renderToBuffer(ps, vc, light, overlay, packColor(r, g, b, a));
    }

    /** {@code EditBox.tick()}: the cursor blink uses the clock now. */
    public static void editBoxTick(net.minecraft.client.gui.components.EditBox box) {}

    public static float getU(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, double u) {
        return sprite.getU((float) (u / 16.0));
    }

    public static float getV(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, double v) {
        return sprite.getV((float) (v / 16.0));
    }

    /** {@code MultiBufferSource.immediate(BufferBuilder)}: the fixed builder is a byte buffer now (the old builder object only supplied memory). */
    public static net.minecraft.client.renderer.MultiBufferSource.BufferSource immediate(CompatBufferBuilder ignored) {
        return net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
    }
}
