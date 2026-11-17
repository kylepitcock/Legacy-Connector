package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.COLLISION_BOXES}): 1.12.2 coremods put a hook first in {@code Block.addCollisionBoxToList} that adds
 * the boxes an entity collides with itself. The modern game asks the block state for one collision shape per entity, which is the same question; the hook
 * fills a list of boxes that becomes the shape. A no-op without such a hook.
 */
@CoremodHook
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateCollisionHookMixin {
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)"
            + "Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodCollision(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!CoremodSites1122.COLLISION_BOXES.listed()) return;
        VoxelShape shape = CoremodSites1122.collisionShape((BlockState) (Object) this, level, pos, context);
        if (shape != null) cir.setReturnValue(shape);
    }
}
