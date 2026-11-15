package org.kylepitcock.legacyconnector.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.kylepitcock.legacyconnector.v1_7_10.client.CoremodClientSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook sites of 1.7.10 on the client (generic, {@code CoremodSites.Client}): the first-person item drawing end ({@code HAND_ITEMS_END}). A no-op
 * without such a hook. The other client places of this round are in {@code ClientPickBlockHookMixin}, {@code MouseTurnHookMixin} and
 * {@code PlayerBlockReachHookMixin}.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ClientCoremodSitesMixin1710 {
    @Inject(method = "renderHandsWithItems", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodHandItemsEnd(float partialTick, PoseStack pose, MultiBufferSource.BufferSource buffers, LocalPlayer player, int light, CallbackInfo ci) {
        if (CoremodSites.Client.HAND_ITEMS_END.listed() || CoremodSites.Client.HAND_RENDERED.listed()) CoremodClientSites.handItemsEnd(partialTick, pose, buffers, light);
    }
}
