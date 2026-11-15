package org.kylepitcock.legacyconnector.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.ARMOR_HIDDEN}): 1.12.2 coremods put a hook in {@code LayerArmorBase.renderArmorLayer} that answers
 * true for an armour piece that is not to be drawn. A no-op without such a hook.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorLayerHiddenHookMixin {
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodArmorHidden(PoseStack pose, MultiBufferSource buffers, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<?> model,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (CoremodSites1122.ARMOR_HIDDEN.listed() && CoremodSites1122.armorHidden(entity, entity.getItemBySlot(slot))) ci.cancel();
    }
}
