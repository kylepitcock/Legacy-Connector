package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.FLYING_TRAVEL_END}): 1.7.10 coremods put a hook last in {@code EntityFlying.moveEntityWithHeading}.
 * A no-op without such a hook.
 */
@CoremodHook
@Mixin(FlyingMob.class)
public abstract class FlyingMobTravelHookMixin {
    @Inject(method = "travel", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodFlyingTravelEnd(Vec3 travel, CallbackInfo ci) {
        if (CoremodSites.FLYING_TRAVEL_END.listed()) CoremodSites.flyingTravelEnd((FlyingMob) (Object) this);
    }
}
