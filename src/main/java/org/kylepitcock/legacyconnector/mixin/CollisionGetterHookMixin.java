package org.kylepitcock.legacyconnector.mixin;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.COLLISION_BOXES}): 1.7.10 coremods took {@code World.getCollidingBoundingBoxes} over (a hook answers the
 * boxes an entity collides with in a region: blocks an ability lets a player pass, blocks kept out of a dimension). The modern game asks the level for the
 * collision shapes of the blocks in a region ({@code getBlockCollisions}: movement, free-space checks); the hook's boxes are those shapes. Only the block
 * part is replaced (the entities' own collisions stay the game's). A no-op without such a hook.
 */
@CoremodHook
@Mixin(CollisionGetter.class)
public interface CollisionGetterHookMixin {
    @Inject(method = "getBlockCollisions", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodCollisionBoxes(Entity entity, AABB region, CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
        if (!CoremodSites.COLLISION_BOXES.listed() || !((Object) this instanceof Level level)) return;
        List<VoxelShape> shapes = CoremodSites.collisionShapes(level, entity, region);
        if (shapes != null) cir.setReturnValue(shapes);
    }
}
