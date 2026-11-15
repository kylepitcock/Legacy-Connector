package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.POTION_PARTICLES}): 1.12.2 coremods put a hook before the potion particles of
 * {@code EntityLivingBase.updatePotionEffects}; when it answers false the method ends there. The game's particle call is the only one of
 * {@code tickEffects} and ends the method. A no-op without such a hook.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectParticlesHookMixin {
    @Inject(method = "tickEffects", require = 0, cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void legacyconnector$coremodPotionParticles(CallbackInfo ci) {
        if (CoremodSites1122.POTION_PARTICLES.listed() && !CoremodSites1122.potionParticles((LivingEntity) (Object) this)) ci.cancel();
    }
}
