package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Leaps at the attack target (the modern goal). */
public class EntityAILeapAtTarget extends GoalTask1122 {
    public EntityAILeapAtTarget(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, float motionY) {
        super(entity, 5, m -> new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal(m, motionY));
    }

}
