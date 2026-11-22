package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyRendererOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.7.10 measured "is the entity close enough to draw" from the camera entity's own position, never from the (moved) third person camera. A legacy mod's
 * {@code EntityRenderer} subclass that pushes the camera far back (Legends' giant suits: 100 blocks) left the player out of the modern range check, so the
 * suit was not drawn at all. Only active while such a camera distance override is set; the frustum test is left alone.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class ViewEntityRangeMixin1710 {
    @WrapOperation(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"), require = 0)
    private <E extends Entity> boolean legacyconnector$viewEntityRange(EntityRenderer<E> renderer, E entity, Frustum frustum, double x, double y, double z, Operation<Boolean> original) {
        if (entity == Minecraft.getInstance().getCameraEntity() && LegacyRendererOverrides.thirdPersonDistance(4.0F) != 4.0F) {
            return original.call(renderer, entity, frustum, entity.getX(), entity.getY(), entity.getZ());
        }
        return original.call(renderer, entity, frustum, x, y, z);
    }
}
