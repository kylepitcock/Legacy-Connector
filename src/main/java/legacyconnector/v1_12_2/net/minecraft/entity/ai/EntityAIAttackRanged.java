package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Shoots the attack target from range (the modern ranged attack goal; the owner is the mob). */
public class EntityAIAttackRanged extends GoalTask1122 {
    public EntityAIAttackRanged(legacyconnector.v1_12_2.net.minecraft.entity.IRangedAttackMob attacker, double speed, int maxAttackTime, float maxAttackDistance) {
        this(attacker, speed, maxAttackTime, maxAttackTime, maxAttackDistance);
    }

    public EntityAIAttackRanged(legacyconnector.v1_12_2.net.minecraft.entity.IRangedAttackMob attacker, double speed, int minAttackTime, int maxAttackTime, float maxAttackDistance) {
        super(attacker instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving l ? l : null, 3, m -> m instanceof net.minecraft.world.entity.monster.RangedAttackMob r
                ? new net.minecraft.world.entity.ai.goal.RangedAttackGoal(r, speed, minAttackTime, maxAttackTime, maxAttackDistance) : null);
    }

}
