package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;

public class EntityAIWander extends GoalTask {
    public EntityAIWander(EntityCreature entity, double speed) {
        super(entity, 1, m -> new RandomStrollGoal(m, speed));
    }
}
