package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.PanicGoal;

public class EntityAIPanic extends GoalTask {
    public EntityAIPanic(EntityCreature entity, double speed) {
        super(entity, 1, m -> new PanicGoal(m, speed));
    }
}
