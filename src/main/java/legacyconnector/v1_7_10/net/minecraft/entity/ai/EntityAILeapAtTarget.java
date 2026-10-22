package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;

public class EntityAILeapAtTarget extends GoalTask {
    public EntityAILeapAtTarget(EntityLiving entity, float leapMotionY) {
        super(entity, 5, m -> new LeapAtTargetGoal(m, leapMotionY));
    }
}
