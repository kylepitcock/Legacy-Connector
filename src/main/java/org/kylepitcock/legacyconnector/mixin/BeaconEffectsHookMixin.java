package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.BEACON_EFFECTS}): 1.12.2 coremods put a hook in {@code TileEntityBeacon.addEffectsToPlayers} that is
 * told the area, the effects, the duration and the amplifier the beacon gives. A no-op without such a hook.
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconEffectsHookMixin {
    @Inject(method = "applyEffects", at = @At("HEAD"), require = 0)
    private static void legacyconnector$coremodBeaconEffects(Level level, BlockPos pos, int beaconLevel, Holder<MobEffect> primary, Holder<MobEffect> secondary,
            CallbackInfo ci) {
        if (CoremodSites1122.BEACON_EFFECTS.listed()) CoremodSites1122.beaconEffects(level, pos, beaconLevel, primary, secondary);
    }
}
