package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;

/** An untamed horse with a rider runs around trying to throw them off; it gains temper and gets tamed when it fails. */
public class EntityAIRunAroundLikeCrazy extends GoalTask {
    public EntityAIRunAroundLikeCrazy(EntityHorse horse, double speed) {
        super(horse, 1, m -> m instanceof net.minecraft.world.entity.animal.horse.AbstractHorse h
                ? new RunAroundLikeCrazyGoal(h, mcc$multiplier(m, speed)) : null);
    }
}
