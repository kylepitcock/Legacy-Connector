package org.kylepitcock.legacyconnector.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.kylepitcock.legacyconnector.v1_7_10.compat.am2.Am2Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class Am2PlayerModelMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("TAIL"))
    private void legacyconnector$rotateAm2Player(AbstractClientPlayer player, PoseStack pose, float bob,
            float bodyYaw, float partialTick, float scale, CallbackInfo ci) {
        float roll = Am2Camera.rollFor(player, partialTick);
        // Vanilla has set the body yaw. Roll around the model's own forward axis.
        if (roll != 0.0F) Am2Camera.rotateModel(pose, roll, player.getBbHeight() / scale);
    }
}
