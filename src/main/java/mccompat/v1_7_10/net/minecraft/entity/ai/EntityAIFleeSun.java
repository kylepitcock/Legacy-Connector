package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;

public class EntityAIFleeSun extends GoalTask {
    public EntityAIFleeSun(EntityCreature entity, double speed) {
        super(entity, 1, m -> new FleeSunGoal(m, speed));
    }
}
