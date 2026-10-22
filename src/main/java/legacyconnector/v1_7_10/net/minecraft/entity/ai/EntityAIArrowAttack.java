package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.IRangedAttackMob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;

/** Ranged attack at an interval while the target is in range (the host forwards to attackEntityWithRangedAttack). */
public class EntityAIArrowAttack extends GoalTask {
    public EntityAIArrowAttack(IRangedAttackMob attacker, double speed, int interval, float range) {
        this(attacker, speed, interval, interval, range);
    }

    public EntityAIArrowAttack(IRangedAttackMob attacker, double speed, int minInterval, int maxInterval, float range) {
        super((EntityLiving) attacker, 3, m -> m instanceof RangedAttackMob r ? new RangedAttackGoal(r, speed, minInterval, maxInterval, range) : null);
    }
}
