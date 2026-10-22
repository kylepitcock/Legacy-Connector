package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

public class EntityAILookIdle extends GoalTask {
    public EntityAILookIdle(EntityLiving entity) {
        super(entity, 3, RandomLookAroundGoal::new);
    }
}
