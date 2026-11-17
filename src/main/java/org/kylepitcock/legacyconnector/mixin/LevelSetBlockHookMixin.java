package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites.SET_BLOCK_REPLACE} and {@code SET_BLOCK_LIMIT}): 1.7.10 coremods changed the block {@code World.setBlock}
 * places and the height above which it places nothing. A no-op without such a hook.
 */
@CoremodHook
@Mixin(Level.class)
public abstract class LevelSetBlockHookMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodSetBlock(BlockPos pos, BlockState state, int flags, int depth, CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites.SET_BLOCK_REPLACE.listed() && !CoremodSites.SET_BLOCK_LIMIT.listed()) return;
        BlockState changed = CoremodSites.setBlock((Level) (Object) this, pos, state);
        if (changed == null) cir.setReturnValue(false);
        else if (changed != state) cir.setReturnValue(((Level) (Object) this).setBlock(pos, changed, flags, depth));
    }
}
