package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook site (generic, {@code CoremodSites.TARGET_CANDIDATES}): 1.7.10 coremods filtered the entities a mob's nearest-target AI picks from (targets
 * that mobs do not see). A target the filter leaves out is not a suitable target here. A no-op without such a hook.
 */
@Mixin(TargetingConditions.class)
public abstract class TargetingCandidatesHookMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodTargetCandidates(LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (CoremodSites.TARGET_CANDIDATES.listed() && attacker instanceof Mob mob && CoremodSites.targetLeftOut(mob, target)) cir.setReturnValue(false);
    }
}
