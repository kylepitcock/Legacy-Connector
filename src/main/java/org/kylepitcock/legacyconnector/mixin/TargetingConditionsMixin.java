package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MOD-SPECIFIC COMPAT (Fisk's Superheroes): its coremod filtered mobs' target searches with {@code SHHelper.isInvisibleToMobs}
 * (invisible, stealthy and tiny heroes are not noticed). See {@code compat.fiskheroes.FiskHeroesHooks}; a no-op without the mod.
 */
@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$fiskHeroesStealth(LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (attacker != null && org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesHooks.invisibleToMobs(target, attacker.distanceToSqr(target))) {
            cir.setReturnValue(false);
        }
    }
}
