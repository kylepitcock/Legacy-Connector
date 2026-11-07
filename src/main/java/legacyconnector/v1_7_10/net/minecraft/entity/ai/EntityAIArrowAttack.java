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

    /** 1.5.2 / 1.4.7 form: the speed is the absolute float speed of the mob (see {@link GoalTask#mcc$multiplier}). */
    public EntityAIArrowAttack(IRangedAttackMob attacker, float speed, int interval, float range) {
        this(attacker, speed, interval, interval, range);
    }

    public EntityAIArrowAttack(IRangedAttackMob attacker, float speed, int minInterval, int maxInterval, float range) {
        super((EntityLiving) attacker, 3, m -> m instanceof RangedAttackMob r ? new RangedAttackGoal(r, mcc$multiplier(m, speed), minInterval, maxInterval, range) : null);
    }

    public EntityAIArrowAttack(IRangedAttackMob attacker, double speed, int minInterval, int maxInterval, float range) {
        super((EntityLiving) attacker, 3, m -> m instanceof RangedAttackMob r ? new RangedAttackGoal(r, speed, minInterval, maxInterval, range) : null);
    }
}
