package org.kylepitcock.legacyconnector.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vanilla head layer draws any non-armor item in the head slot as a floating 3D item. Legacy armor is drawn by
 * {@code LegacyArmorLayer} instead, so the head layer skips it. Done here rather than by replacing the layer on the renderer:
 * other mods look up the real {@link CustomHeadLayer} in the renderer's layer list (#228). A no-op without legacy items.
 */
@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerLegacyArmorMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void legacyconnector$skipLegacyArmor(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (org.kylepitcock.legacyconnector.v1_7_10.client.LegacyArmorLayer.drawsHead(entity)) ci.cancel();
    }
}
