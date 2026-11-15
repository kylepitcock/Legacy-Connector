package org.kylepitcock.legacyconnector.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.kylepitcock.legacyconnector.v1_7_10.client.CoremodClientSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook sites (generic, {@code CoremodSites.Client.LIMB_SWING_RENDER}, {@code LIMB_AMOUNT_RENDER}, {@code ROTATE_CORPSE}): 1.7.10 coremods changed the
 * limb swing a model is posed with and rotated the player model with GL calls. A no-op without such hooks.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingRenderPoseHookMixin1710 {
    private static final String SETUP_ANIM = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V";

    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = SETUP_ANIM), require = 0)
    private void legacyconnector$coremodLimbSwing(net.minecraft.client.model.EntityModel<?> model, net.minecraft.world.entity.Entity entity, float swing, float amount, float age,
            float yaw, float pitch, Operation<Void> original) {
        if (entity instanceof LivingEntity living && (CoremodSites.Client.LIMB_SWING_RENDER.listed() || CoremodSites.Client.LIMB_AMOUNT_RENDER.listed())) {
            swing = CoremodSites.Client.limb(CoremodSites.Client.LIMB_SWING_RENDER, living, swing);
            amount = CoremodSites.Client.limb(CoremodSites.Client.LIMB_AMOUNT_RENDER, living, amount);
        }
        original.call(model, entity, swing, amount, age, yaw, pitch);
    }

    @Inject(method = "setupRotations", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodRotateCorpse(LivingEntity entity, PoseStack pose, float ageInTicks, float rotationYaw, float partialTick, float scale, CallbackInfo ci) {
        if (entity instanceof Player player && CoremodSites.Client.ROTATE_CORPSE.listed()) CoremodClientSites.rotateCorpse(player, pose, partialTick);
    }
}
