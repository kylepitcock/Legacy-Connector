package org.kylepitcock.legacyconnector.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.kylepitcock.legacyconnector.v1_12_2.client.EntityFireHook1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.ENTITY_FIRE}): 1.12.2 coremods put a hook first in {@code Render.renderEntityOnFire} that answers
 * true when it drew the flames itself. A no-op without such a hook.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityFlameHookMixin {
    @Inject(method = "renderFlame", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodFlame(PoseStack pose, MultiBufferSource buffers, Entity entity, Quaternionf rotation, CallbackInfo ci) {
        if (CoremodSites1122.ENTITY_FIRE.listed() && EntityFireHook1122.draw(entity, pose, buffers)) ci.cancel();
    }
}
