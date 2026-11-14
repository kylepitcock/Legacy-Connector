package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook sites (generic, {@code CoremodSites.NEIGHBOR_POWERED}, {@code POWER_LEVEL}): 1.7.10 coremods changed what
 * {@code World.isBlockIndirectlyGettingPowered} and {@code getIndirectPowerLevelTo} answer (powers that emit redstone signals). Only real levels;
 * a no-op without such hooks.
 */
@Mixin(SignalGetter.class)
public interface SignalGetterHookMixin {
    @Inject(method = "hasNeighborSignal", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodNeighborPowered(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites.NEIGHBOR_POWERED.listed() || !((Object) this instanceof Level level)) return;
        boolean powered = cir.getReturnValueZ();
        boolean changed = CoremodSites.neighborPowered(level, pos, powered);
        if (changed != powered) cir.setReturnValue(changed);
    }

    @Inject(method = "getSignal", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodPowerLevel(BlockPos pos, Direction side, CallbackInfoReturnable<Integer> cir) {
        if (!CoremodSites.POWER_LEVEL.listed() || !((Object) this instanceof Level level)) return;
        int power = cir.getReturnValueI();
        int changed = CoremodSites.powerLevel(level, pos, side, power);
        if (changed != power) cir.setReturnValue(changed);
    }
}
