package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.Client.MOUSE_OVER}): 1.7.10 coremods put a hook into {@code EntityRenderer.getMouseOver} right after the
 * entity looked at was cleared; one answering true leaves the picking out and has set the target to a miss (no interaction while rowing in third person,
 * for example). Here the target becomes a miss and the game's picking is skipped, where the game would pick (a level, a player, a camera entity). A no-op
 * without such a hook.
 */
@CoremodHook
@Mixin(GameRenderer.class)
public abstract class GameRendererPickHookMixin {
    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "pick(F)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodMouseOver(float partialTicks, CallbackInfo ci) {
        if (!CoremodSites.Client.MOUSE_OVER.listed() || minecraft.level == null || minecraft.player == null || minecraft.getCameraEntity() == null) return;
        if (!CoremodSites.Client.mouseOverTaken()) return;
        minecraft.hitResult = BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO);
        minecraft.crosshairPickEntity = null;
        ci.cancel();
    }
}
