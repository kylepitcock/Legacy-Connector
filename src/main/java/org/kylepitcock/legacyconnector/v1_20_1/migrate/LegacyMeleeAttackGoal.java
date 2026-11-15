package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Stands in for 1.20.1's {@code MeleeAttackGoal}. 1.20.5 dropped {@code getAttackReachSqr(target)} and {@code checkAndPerformAttack(target, distanceSqr)}:
 * the reach is the mob's own melee range now and the goal calls {@code checkAndPerformAttack(target)}. A 1.20.1 goal overrides either (animated mobs time
 * their attack in {@code checkAndPerformAttack}, big mobs widen {@code getAttackReachSqr}), so the stand-in keeps both, with the 1.20.1 defaults, and the
 * 1.21.1 hook calls the 1.20.1 one with the squared distance the 1.20.1 {@code tick} passed.
 */
public class LegacyMeleeAttackGoal extends MeleeAttackGoal {
    public LegacyMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
    }

    /** 1.21.1's {@code tick} ends with this call; 1.20.1's passed the larger of the squared distances to the target's entity and position ({@code getPerceivedTargetDistanceSquareForMeleeAttack}). */
    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        checkAndPerformAttack(target, Math.max(mob.distanceToSqr(target), mob.distanceToSqr(target.position())));
    }

    /** 1.20.1's attack: when in reach and the cooldown is over, swing and hit. */
    protected void checkAndPerformAttack(LivingEntity target, double distanceSqr) {
        if (distanceSqr <= getAttackReachSqr(target) && isTimeToAttack()) {
            resetAttackCooldown();
            mob.swing(InteractionHand.MAIN_HAND);
            mob.doHurtTarget(target);
        }
    }

    /** 1.20.1's reach: twice the mob's width squared plus the target's width. */
    protected double getAttackReachSqr(LivingEntity target) {
        return mob.getBbWidth() * 2.0F * mob.getBbWidth() * 2.0F + target.getBbWidth();
    }
}
