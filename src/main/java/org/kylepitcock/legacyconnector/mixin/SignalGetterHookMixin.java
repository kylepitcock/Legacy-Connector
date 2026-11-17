package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites.NEIGHBOR_POWERED}, {@code POWER_LEVEL}): 1.7.10 coremods changed what
 * {@code World.isBlockIndirectlyGettingPowered} and {@code getIndirectPowerLevelTo} answer (powers that emit redstone signals). Only real levels;
 * a no-op without such hooks.
 */
@CoremodHook
@Mixin(SignalGetter.class)
public interface SignalGetterHookMixin {
    @Inject(method = "hasNeighborSignal", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodNeighborPowered(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites.NEIGHBOR_POWERED.listed() || !((Object) this instanceof Level level)) return;
        boolean powered = cir.getReturnValueZ();
        boolean changed = CoremodSites.neighborPowered(level, pos, powered);
        if (changed != powered) cir.setReturnValue(changed);
    }

    /** 1.12.2 coremods (CoremodSites1122.REDSTONE_POWER): a hook first in {@code World.getRedstonePower} answering a power other than 0 is the answer. */
    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodRedstonePower(BlockPos pos, Direction side, CallbackInfoReturnable<Integer> cir) {
        if (!CoremodSites1122.REDSTONE_POWER.listed() || !((Object) this instanceof Level level)) return;
        int power = CoremodSites1122.power(CoremodSites1122.REDSTONE_POWER, level, pos, side);
        if (power != 0) cir.setReturnValue(power);
    }

    /** {@code CoremodSites1122.STRONG_POWER}: the same for {@code World.getStrongPower} (modern {@code getDirectSignal}). */
    @Inject(method = "getDirectSignal", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodStrongPower(BlockPos pos, Direction side, CallbackInfoReturnable<Integer> cir) {
        if (!CoremodSites1122.STRONG_POWER.listed() || !((Object) this instanceof Level level)) return;
        int power = CoremodSites1122.power(CoremodSites1122.STRONG_POWER, level, pos, side);
        if (power != 0) cir.setReturnValue(power);
    }

    @Inject(method = "getSignal", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodPowerLevel(BlockPos pos, Direction side, CallbackInfoReturnable<Integer> cir) {
        if (!CoremodSites.POWER_LEVEL.listed() || !((Object) this instanceof Level level)) return;
        int power = cir.getReturnValueI();
        int changed = CoremodSites.powerLevel(level, pos, side, power);
        if (changed != power) cir.setReturnValue(changed);
    }
}
