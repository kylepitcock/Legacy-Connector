package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyRendererOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The "inside a block" screen overlay of a player whose camera a legacy mod's {@code EntityRenderer} subclass raised (see
 * {@link LegacyRendererOverrides#cameraRaise}): 1.7.10 tested the eye the mod set, here the camera is lifted but the player's own eye stays at the
 * normal height, where the block under a giant's feet (or Pehkui's wider sampling for a big legacy eye height) filled the screen. A no-op without such
 * a renderer. {@code getOverlayBlock} is NeoForge's version of the block lookup (it also yields the position); {@code getViewBlockingState} reads it.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectLegacyEyeMixin1710 {
    @Inject(method = "getOverlayBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacyconnector$overlayAtCamera(Player player, CallbackInfoReturnable<Pair<BlockState, BlockPos>> cir) {
        double raise = LegacyRendererOverrides.cameraRaise(player);
        if (raise == 0.0) return;
        cir.setReturnValue(LegacyRendererOverrides.overlayBlock(player, raise));
    }
}
