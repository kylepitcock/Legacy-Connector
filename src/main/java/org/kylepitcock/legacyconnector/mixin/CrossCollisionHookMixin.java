package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites.FENCE_BOXES} and {@code PANE_IGNORED}): 1.7.10 coremods changed the collision boxes of fences and
 * panes for some entities (walking through them, passing under them). A no-op without such a hook.
 */
@CoremodHook
@Mixin(CrossCollisionBlock.class)
public abstract class CrossCollisionHookMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodCrossShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!CoremodSites.FENCE_BOXES.listed() && !CoremodSites.PANE_IGNORED.listed()) return;
        VoxelShape shape = CoremodSites.crossShape(state, level, pos, context);
        if (shape != null) cir.setReturnValue(shape);
    }
}
