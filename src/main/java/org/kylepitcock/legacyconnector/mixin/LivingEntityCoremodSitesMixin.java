package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites.POTION_PARTICLE_NAME}, {@code ARMOR_VISIBILITY}, {@code LIMB_SWING}): 1.7.10 coremods put hooks into
 * {@code EntityLivingBase.updatePotionEffects} (the name of the effect particle), {@code EntityPlayer.getArmorVisibility} and the limb swing distance of
 * {@code moveEntityWithHeading}. No-ops without such hooks.
 */
@CoremodHook
@Mixin(LivingEntity.class)
public abstract class LivingEntityCoremodSitesMixin {
    @ModifyArg(method = "tickEffects", require = 0, index = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private ParticleOptions legacyconnector$coremodPotionParticleName(ParticleOptions particle) {
        return CoremodSites.POTION_PARTICLE_NAME.listed() ? CoremodSites.potionParticle((LivingEntity) (Object) this, particle) : particle;
    }

    @Inject(method = "getArmorCoverPercentage", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodArmorVisibility(CallbackInfoReturnable<Float> cir) {
        if (CoremodSites.ARMOR_VISIBILITY.listed()) cir.setReturnValue(CoremodSites.armorVisibility((LivingEntity) (Object) this, cir.getReturnValueF()));
    }

    @ModifyVariable(method = "calculateEntityAnimation", at = @At("STORE"), ordinal = 0, require = 0)
    private float legacyconnector$coremodLimbSwing(float distance) {
        return CoremodSites.LIMB_SWING.listed() ? CoremodSites.limbSwing((LivingEntity) (Object) this, distance) : distance;
    }
}
