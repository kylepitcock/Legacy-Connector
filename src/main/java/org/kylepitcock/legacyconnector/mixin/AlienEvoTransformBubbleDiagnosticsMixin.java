package org.kylepitcock.legacyconnector.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.AlienEvoTransformBubbleDiagnostics1201Runtime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional observations for AlienEvo's client-only transform-bubble renderer.
 * Redirects call the original PoseStack method with unchanged values.
 */
@Pseudo
@Mixin(targets = "net.habb.alienevo.client.energybeam.TransformBubbleRenderer", remap = false)
abstract class AlienEvoTransformBubbleDiagnosticsMixin {
    private static final String RENDER_METHOD =
            "render(Lnet/minecraft/client/player/AbstractClientPlayer;"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;F"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;IZF)V";
    private static final String SPHERE_METHOD =
            "renderTransformSphere(Lnet/minecraft/client/player/AbstractClientPlayer;F"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;FZ)V";
    private static final String POSE_TRANSLATE =
            "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V";

    @Inject(method = RENDER_METHOD, at = @At("HEAD"), remap = false, require = 1)
    private void legacyconnector$traceRenderEntry(
            AbstractClientPlayer player, Vec3 from, Vec3 target, float progress,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            boolean firstPerson, float partialTick, CallbackInfo ci) {
        AlienEvoTransformBubbleDiagnostics1201Runtime.traceRenderEntry(
                player, from, target, progress, poseStack, firstPerson, partialTick);
    }

    @Inject(method = SPHERE_METHOD, at = @At("HEAD"), remap = false, require = 1)
    private void legacyconnector$beginTransformBubbleTrace(
            AbstractClientPlayer player, float progress, PoseStack poseStack,
            MultiBufferSource buffers, float partialTick, boolean firstPerson, CallbackInfo ci) {
        AlienEvoTransformBubbleDiagnostics1201Runtime.beginTransformSphere(
                player, progress, partialTick, firstPerson);
    }

    @Inject(method = SPHERE_METHOD, at = @At("RETURN"), remap = false, require = 1)
    private void legacyconnector$endTransformBubbleTrace(
            AbstractClientPlayer player, float progress, PoseStack poseStack,
            MultiBufferSource buffers, float partialTick, boolean firstPerson, CallbackInfo ci) {
        AlienEvoTransformBubbleDiagnostics1201Runtime.endTransformSphere();
    }

    @Redirect(method = SPHERE_METHOD,
            at = @At(value = "INVOKE", target = POSE_TRANSLATE, ordinal = 0, remap = false),
            remap = false, require = 1)
    private void legacyconnector$traceHeadCorrection(PoseStack poseStack, double x, double y, double z) {
        poseStack.translate(x, y, z);
        AlienEvoTransformBubbleDiagnostics1201Runtime.traceHeadCorrection(poseStack, x, y, z);
    }

    @Redirect(method = SPHERE_METHOD,
            at = @At(value = "INVOKE", target = POSE_TRANSLATE, ordinal = 1, remap = false),
            remap = false, require = 1)
    private void legacyconnector$traceFirstPersonCameraOffset(
            PoseStack poseStack, double x, double y, double z) {
        poseStack.translate(x, y, z);
        AlienEvoTransformBubbleDiagnostics1201Runtime.traceCameraOffset(poseStack, x, y, z);
    }
}
