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
        // the constants themselves (hooks that stand in for 0.08, 0.04, 0.05)
        if (org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.gravityConstantListed()) scaled = org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.gravityConstant(self, scaled);
        if (scaled != result) cir.setReturnValue(scaled);
    }

    /** Coremod hook site (generic, {@code CoremodSites.SPRINT_PARTICLES}): 1.7.10 coremods changed the sprint check of the particles in {@code onEntityUpdate}. */
    @Inject(method = "canSpawnSprintParticle", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodSprintParticles(CallbackInfoReturnable<Boolean> cir) {
        if (!org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.SPRINT_PARTICLES.listed()) return;
        boolean sprinting = cir.getReturnValueZ();
        boolean changed = org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.sprintParticles((Entity) (Object) this, sprinting);
        if (changed != sprinting) cir.setReturnValue(changed);
    }
}
