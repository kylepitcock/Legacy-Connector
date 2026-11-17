package org.kylepitcock.legacyconnector.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.SHOULDER_TEXTURE}): 1.12.2 coremods put a hook in {@code LayerEntityOnShoulder.renderEntityOnShoulder}
 * that answers the texture of the parrot from its saved tag. The tag of the shoulder being drawn is kept while the layer draws it, for
 * the parrot texture mixin. A no-op without such a hook.
 */
@CoremodHook
@Mixin(ParrotOnShoulderLayer.class)
public abstract class ShoulderParrotTextureHookMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/player/Player;FFFFZ)V", at = @At("HEAD"), require = 0)
    private void legacyconnector$coremodShoulderTag(PoseStack pose, MultiBufferSource buffers, int light, Player player, float a, float b, float c, float d,
            boolean left, CallbackInfo ci) {
        if (CoremodSites1122.SHOULDER_TEXTURE.listed()) {
            CompoundTag tag = left ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
            CoremodSites1122.SHOULDER_TAG.set(tag);
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/player/Player;FFFFZ)V", at = @At("RETURN"), require = 0)
    private void legacyconnector$coremodShoulderTagEnd(PoseStack pose, MultiBufferSource buffers, int light, Player player, float a, float b, float c, float d,
            boolean left, CallbackInfo ci) {
        CoremodSites1122.SHOULDER_TAG.remove();
    }
}
