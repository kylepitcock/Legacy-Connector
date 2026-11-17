package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites1122.EXHAUSTION_DIVE/SWIM/SPRINT/CROUCH/WALK}): 1.12.2 coremods that rework hunger put a hook at each
 * exhaustion call of {@code EntityPlayer.addMovementStat}; the hook answers the amount and is told which movement it is by the place. The modern method
 * has the same calls in another order (swimming, diving, in water, sprinting, crouching, walking). No-ops without hooks.
 */
@CoremodHook
@Mixin(ServerPlayer.class)
public abstract class PlayerMovementExhaustionHookMixin {
    private static final String CAUSE = "causeFoodExhaustion(F)V";

    @ModifyArg(method = "checkMovementStatistics", at = @At(value = "INVOKE", target = CAUSE, ordinal = 0), index = 0, require = 0)
    private float legacyconnector$coremodSwimming(float amount) {
        return legacyconnector$exhaust(CoremodSites1122.EXHAUSTION_SWIM, amount);
    }

    @ModifyArg(method = "checkMovementStatistics", at = @At(value = "INVOKE", target = CAUSE, ordinal = 1), index = 0, require = 0)
    private float legacyconnector$coremodDiving(float amount) {
        return legacyconnector$exhaust(CoremodSites1122.EXHAUSTION_DIVE, amount);
    }

    @ModifyArg(method = "checkMovementStatistics", at = @At(value = "INVOKE", target = CAUSE, ordinal = 2), index = 0, require = 0)
    private float legacyconnector$coremodInWater(float amount) {
        return legacyconnector$exhaust(CoremodSites1122.EXHAUSTION_SWIM, amount);
    }

    @ModifyArg(method = "checkMovementStatistics", at = @At(value = "INVOKE", target = CAUSE, ordinal = 3), index = 0, require = 0)
    private float legacyconnector$coremodSprinting(float amount) {
        return legacyconnector$exhaust(CoremodSites1122.EXHAUSTION_SPRINT, amount);
    }

    @ModifyArg(method = "checkMovementStatistics", at = @At(value = "INVOKE", target = CAUSE, ordinal = 4), index = 0, require = 0)
    private float legacyconnector$coremodCrouching(float amount) {
        return legacyconnector$exhaust(CoremodSites1122.EXHAUSTION_CROUCH, amount);
    }

    @ModifyArg(method = "checkMovementStatistics", at = @At(value = "INVOKE", target = CAUSE, ordinal = 5), index = 0, require = 0)
    private float legacyconnector$coremodWalking(float amount) {
        return legacyconnector$exhaust(CoremodSites1122.EXHAUSTION_WALK, amount);
    }

    private float legacyconnector$exhaust(org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.Site site, float amount) {
        return site.listed() ? CoremodSites1122.exhaustion(site, (ServerPlayer) (Object) this, amount) : amount;
    }
}
