package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.FALL_THROUGH}): 1.12.2 coremods put a hook last in {@code BlockFalling.canFallThrough} that answers
 * for a state (blocks that gravity blocks fall through). A no-op without such a hook.
 */
@CoremodHook
@Mixin(FallingBlock.class)
public abstract class FallingBlockFreeHookMixin {
    @Inject(method = "isFree", at = @At("RETURN"), cancellable = true, require = 0)
    private static void legacyconnector$coremodFallThrough(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites1122.FALL_THROUGH.listed()) return;
        boolean value = cir.getReturnValueZ();
        boolean changed = CoremodSites1122.fallThrough(state, value);
        if (changed != value) cir.setReturnValue(changed);
    }
}
