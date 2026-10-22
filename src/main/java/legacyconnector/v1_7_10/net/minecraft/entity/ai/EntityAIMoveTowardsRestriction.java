package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;

public class EntityAIMoveTowardsRestriction extends GoalTask {
    public EntityAIMoveTowardsRestriction(EntityCreature entity, double speed) {
        super(entity, 1, m -> new MoveTowardsRestrictionGoal(m, speed));
    }
}
