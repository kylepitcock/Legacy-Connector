package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;

public class EntityAIMoveTowardsTarget extends GoalTask {
    public EntityAIMoveTowardsTarget(EntityCreature entity, double speed, float maxDistance) {
        super(entity, 1, m -> new MoveTowardsTargetGoal(m, speed, maxDistance));
    }
}
