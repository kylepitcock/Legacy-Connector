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

    /** The old {@code float} colour channels of a packed ARGB {@code int} (1.21.1 passes one packed colour where 1.20.1 took four floats). */
    public static float colorRed(int argb) {
        return (argb >> 16 & 255) / 255.0F;
    }

    public static float colorGreen(int argb) {
        return (argb >> 8 & 255) / 255.0F;
    }

    public static float colorBlue(int argb) {
        return (argb & 255) / 255.0F;
    }

    public static float colorAlpha(int argb) {
        return (argb >>> 24) / 255.0F;
    }

    /** An {@code int} parameter 1.21.1 dropped (a tick count such as {@code ChatComponent.addMessage}'s): zero. */
    public static int zeroInt() {
        return 0;
    }

    /** The {@code CompoundTag} parameter 1.21.1 removed from {@code Mob.finalizeSpawn} (spawn-time entity data): none. */
    public static net.minecraft.nbt.CompoundTag nullTag() {
        return null;
    }

    /** {@code appendHoverText(ItemStack, Level, ...)} became {@code (ItemStack, Item.TooltipContext, ...)}: the context's level (null on the main menu, like before). */
    public static net.minecraft.world.level.Level levelOf(net.minecraft.world.item.Item.TooltipContext context) {
        return context.level();
    }

    /** A {@code boolean} parameter 1.21.1 dropped: false. */
    public static boolean noFlag() {
        return false;
    }

    /** 1.20.1's {@code onDisconnect(Component reason)}: the reason of 1.21.1's {@code DisconnectionDetails}. */
    public static net.minecraft.network.chat.Component disconnectReason(net.minecraft.network.DisconnectionDetails details) {
        return details.reason();
    }

    /** 1.20.1 passed a {@code MobEffect}, 1.21.1 passes its holder. */
    public static net.minecraft.world.effect.MobEffect effectOf(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return effect.value();
    }

    /**
     * A target that returned a value in 1.20.1 and returns void now: the old handler takes a {@code CallbackInfoReturnable}, made from the new target's
     * {@code CallbackInfo} (same id and cancellability, no value).
     */
    public static org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Object> returnableOf(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        return new org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<>(ci.getId(), ci.isCancellable(), null);
    }

    /** After the old handler ran: a cancelled returnable cancels the info (the return value of a void target is dropped). */
    public static void propagate(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Object> cir, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (cir.isCancelled() && ci.isCancellable()) ci.cancel();
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
