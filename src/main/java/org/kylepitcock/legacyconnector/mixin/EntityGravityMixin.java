package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook site (generic, {@code CoremodSites.GRAVITY}): 1.7.10 coremods that changed the gravity constant of an entity's movement are called
 * here. Without the hook list: MOD-SPECIFIC COMPAT (Fisk's Superheroes): its coremod scaled the gravity constant of every entity with
 * {@code ASMHooks.getEntityGravity} (Moon and Quantum Realm gravity, gravity manipulation). See
 * {@code compat.fiskheroes.FiskHeroesHooks}; a no-op without the mod.
 */
@Mixin(Entity.class)
public abstract class EntityGravityMixin {
    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$fiskHeroesGravity(CallbackInfoReturnable<Double> cir) {
        double result = cir.getReturnValueD();
        Entity self = (Entity) (Object) this;
        double scaled = org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.GRAVITY.listed() ? org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.gravity(self, result)
                : org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesHooks.gravity(self, result);
        if (scaled != result) cir.setReturnValue(scaled);
    }
}
