package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites.TRAVEL}): any 1.7.10 coremod that put a cancelling hook first in
 * {@code EntityLivingBase.moveEntityWithHeading} is called here. Without the hook list (no 1.7.10 client jar to read the coremods against):
 * MOD-SPECIFIC COMPAT (Fisk's Superheroes): its coremod called {@code ASMHooks.moveEntityWithHeading} first thing in
 * {@code EntityLivingBase.moveEntityWithHeading}; heroes fly, glide and crawl up walls through it. See
 * {@code compat.fiskheroes.FiskHeroesHooks}; a no-op without the mod.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void legacyconnector$fiskHeroesTravel(Vec3 travel, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.TRAVEL.listed() ? org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.travel(self, travel)
                : org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesHooks.moveEntityWithHeading(self, travel)) ci.cancel();
    }
}
