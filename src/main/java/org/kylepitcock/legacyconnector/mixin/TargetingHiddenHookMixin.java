package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.TARGET_HIDDEN}): 1.12.2 coremods put a hook first in {@code EntityAITarget.isSuitableTarget}; one
 * answering true makes the target unsuitable for the mob. A no-op without such a hook.
 */
@CoremodHook
@Mixin(TargetingConditions.class)
public abstract class TargetingHiddenHookMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodTargetHidden(LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (attacker instanceof Mob mob && CoremodSites1122.TARGET_HIDDEN.listed() && CoremodSites1122.targetHidden(mob, target)) cir.setReturnValue(false);
    }
}
