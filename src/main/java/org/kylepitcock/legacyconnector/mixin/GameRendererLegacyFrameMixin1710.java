package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyRendererOverrides;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Legacy mods that replaced {@code Minecraft.entityRenderer} with a subclass (see {@link LegacyRendererOverrides}): the frame runs through its
 * {@code updateCameraAndRender}, which calls the modern frame as its {@code super}. A no-op without such a subclass.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererLegacyFrameMixin1710 {
    @WrapMethod(method = "render", require = 0)
    private void legacyconnector$legacyFrame(DeltaTracker deltaTracker, boolean renderLevel, Operation<Void> original) {
        if (!LegacyRendererOverrides.frame(deltaTracker.getGameTimeDeltaPartialTick(false), () -> original.call(deltaTracker, renderLevel))) {
            original.call(deltaTracker, renderLevel);
        }
    }
}
