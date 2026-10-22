package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;

public class EntityAIRestrictSun extends GoalTask {
    public EntityAIRestrictSun(EntityCreature entity) {
        super(entity, 0, RestrictSunGoal::new);
    }
}
