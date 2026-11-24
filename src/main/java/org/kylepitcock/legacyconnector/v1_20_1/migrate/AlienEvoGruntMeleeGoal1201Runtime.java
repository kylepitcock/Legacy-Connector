package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Objects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;

/**
 * Runtime replacement for the vanilla 1.20.1 MeleeAttackGoal reach helper,
 * removed from the 1.21.1 class.
 */
public final class AlienEvoGruntMeleeGoal1201Runtime {
    private AlienEvoGruntMeleeGoal1201Runtime() {}

    public static double baseAttackReachSqr(Mob mob, LivingEntity target) {
        Objects.requireNonNull(mob, "mob");
        Objects.requireNonNull(target, "target");
        return baseAttackReachSqr(mob.getBbWidth(), target.getBbWidth());
    }

    static double baseAttackReachSqr(float mobWidth, float targetWidth) {
        // Preserve the legacy float arithmetic and the target-width allowance.
        return (double) (mobWidth * 2.0F * mobWidth * 2.0F + targetWidth);
    }
}
