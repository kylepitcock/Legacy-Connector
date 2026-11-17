package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites1122.MOVE_START} and {@code MOVE_DONE}): 1.12.2 coremods put a hook first in {@code Entity.move} (it notes
 * the entity's motion) and one after the block collisions (it sees the movement that happened and the one that was asked for: the position change and the
 * move argument). A no-op without such hooks.
 */
@CoremodHook
@Mixin(Entity.class)
public abstract class EntityMoveHookMixin {
    @Unique
    private double legacyconnector$moveX, legacyconnector$moveY, legacyconnector$moveZ;

    @Inject(method = "move", at = @At("HEAD"), require = 0)
    private void legacyconnector$coremodMoveStart(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (!CoremodSites1122.MOVE_START.listed() && !CoremodSites1122.MOVE_DONE.listed()) return;
        Entity self = (Entity) (Object) this;
        legacyconnector$moveX = self.getX();
        legacyconnector$moveY = self.getY();
        legacyconnector$moveZ = self.getZ();
        if (CoremodSites1122.MOVE_START.listed()) CoremodSites1122.moveStart(self);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tryCheckInsideBlocks()V"), require = 0)
    private void legacyconnector$coremodMoveDone(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (!CoremodSites1122.MOVE_DONE.listed()) return;
        Entity self = (Entity) (Object) this;
        CoremodSites1122.moveDone(self, self.getX() - legacyconnector$moveX, self.getY() - legacyconnector$moveY, self.getZ() - legacyconnector$moveZ,
                movement.x, movement.y, movement.z);
    }
}
