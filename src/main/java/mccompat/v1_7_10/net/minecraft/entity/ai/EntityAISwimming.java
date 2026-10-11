package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.FloatGoal;

public class EntityAISwimming extends GoalTask {
    public EntityAISwimming(EntityLiving entity) {
        super(entity, 4, FloatGoal::new);
        if (entity.getNavigator() != null) entity.getNavigator().setCanSwim(true);
    }
}
