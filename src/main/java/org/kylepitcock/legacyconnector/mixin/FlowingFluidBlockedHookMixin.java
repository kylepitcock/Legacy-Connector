package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.LIQUID_BLOCKED}): 1.12.2 coremods put hooks into {@code BlockDynamicLiquid.isBlocked} (the check
 * of blocks flowing liquid does not replace); one answering true keeps the block. A no-op without such a hook.
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidBlockedHookMixin {
    @Inject(method = "canHoldFluid", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodLiquidBlocked(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (CoremodSites1122.LIQUID_BLOCKED.listed() && cir.getReturnValueZ() && CoremodSites1122.liquidBlocked(state)) cir.setReturnValue(false);
    }
}
