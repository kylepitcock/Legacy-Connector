package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MOD-SPECIFIC COMPAT (Fisk's Superheroes): its coremod scaled the gravity constant of every entity with
 * {@code ASMHooks.getEntityGravity} (Moon and Quantum Realm gravity, gravity manipulation). See
 * {@code compat.fiskheroes.FiskHeroesHooks}; a no-op without the mod.
 */
@Mixin(Entity.class)
public abstract class EntityGravityMixin {
    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$fiskHeroesGravity(CallbackInfoReturnable<Double> cir) {
        double result = cir.getReturnValueD();
        double scaled = org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesHooks.gravity((Entity) (Object) this, result);
        if (scaled != result) cir.setReturnValue(scaled);
    }
}
