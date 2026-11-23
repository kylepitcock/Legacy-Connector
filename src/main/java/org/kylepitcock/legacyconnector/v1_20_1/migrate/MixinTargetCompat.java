package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;

/**
 * Helpers of {@code mixin-targets.tsv}: the old arguments a 1.20.1 mixin handler expects, rebuilt from what the 1.21.1 target method passes
 * (see {@code TargetRemap} in the loader), and the Forge particle render call.
 */
public final class MixinTargetCompat {
    private MixinTargetCompat() {}

    /** The {@code PoseStack} parameter 1.21.1 dropped from {@code LevelRenderer.renderLevel} and {@code ParticleEngine.render}: an empty stack (the camera matrices are passed separately now). */
    public static PoseStack freshPoseStack() {
        return new PoseStack();
    }

    /** {@code renderLevel}'s {@code float partialTick}: the delta tracker's. */
    public static float partialTick(DeltaTracker tracker) {
        return tracker.getGameTimeDeltaPartialTick(false);
    }

    /** {@code renderLevel}'s {@code long finishNanoTime} (a frame limiter budget): gone in 1.21.1, no deadline. */
    public static long noTime() {
        return 0L;
    }

    /** The buffer source Forge's {@code ParticleEngine.render} took (the level renderer's). */
    public static MultiBufferSource.BufferSource particleBuffers() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    /**
     * Forge 1.20.1's {@code ParticleEngine.render(PoseStack, BufferSource, LightTexture, Camera, float, Frustum)} drew the particles before the entities (the
     * opaque ones; the vanilla pass followed). 1.21.1's engine splits the passes itself by {@code ParticleRenderType.isTranslucent}: the call draws the
     * opaque types and the types of mods (which the 1.20.1 call drew too and filter themselves, like Photon's layers), vanilla's translucent ones come in the
     * level renderer's own passes.
     */
    public static void particleRenderOpaque(ParticleEngine engine, PoseStack poseStack, MultiBufferSource.BufferSource buffers, LightTexture light, Camera camera,
            float partialTick, Frustum frustum) {
        engine.render(light, camera, partialTick, frustum, type -> !type.isTranslucent() || !type.getClass().getName().startsWith("net.minecraft."));
    }

    /**
     * A mod particle's 1.20.1 {@code shouldCull()} (Forge's frustum switch of {@code ParticleEngine.render}) as the 1.21.1 render box: 1.21.1's engine culls by
     * {@code getRenderBoundingBox} alone, so a particle that opted out of culling gets an unbounded box; one that wants culling keeps its bounding box.
     */
    public static net.minecraft.world.phys.AABB renderBox(boolean shouldCull, Object particle) {
        return shouldCull ? ((net.minecraft.client.particle.Particle) particle).getBoundingBox() : net.minecraft.world.phys.AABB.INFINITE;
    }

    /** {@code BufferUploader.drawWithShader(RenderedBuffer)} of a builder that wrote no vertex: 1.21.1's {@code build()} answers null for it, 1.20.1's empty buffer drew nothing. */
    public static void uploadShader(com.mojang.blaze3d.vertex.MeshData mesh) {
        if (mesh != null) com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
    }

    /** {@code BufferUploader.draw(RenderedBuffer)}, see {@link #uploadShader}. */
    public static void upload(com.mojang.blaze3d.vertex.MeshData mesh) {
        if (mesh != null) com.mojang.blaze3d.vertex.BufferUploader.draw(mesh);
    }
}
