package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook sites (generic, {@code CoremodSites.STEP_DOWN} and {@code NOCLIP_MOVE}): 1.7.10 coremods changed the vertical movement left after
 * {@code Entity.moveEntity}'s collisions (stepping down) and moved entities without collisions themselves. A no-op without such a hook.
 */
@Mixin(Entity.class)
public abstract class EntityMovementHookMixin {
    @Inject(method = "collide", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodStepDown(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (!CoremodSites.STEP_DOWN.listed()) return;
        Vec3 collided = cir.getReturnValue();
        double y = CoremodSites.stepDown((Entity) (Object) this, collided.y, movement.y);
        if (y != collided.y) cir.setReturnValue(new Vec3(collided.x, y, collided.z));
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodNoClipMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.noPhysics && CoremodSites.NOCLIP_MOVE.listed() && CoremodSites.noClipMove(self, movement)) ci.cancel();
    }
}
