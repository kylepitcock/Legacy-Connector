package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;

public class EntityAIWatchClosest extends GoalTask {
    public EntityAIWatchClosest(EntityLiving entity, Class<?> watchTarget, float distance) {
        this(entity, watchTarget, distance, 0.02F);
    }

    public EntityAIWatchClosest(EntityLiving entity, Class<?> watchTarget, float distance, float chance) {
        super(entity, 2, m -> new LookAtPlayerGoal(m, LegacyTargets.search(watchTarget), distance, chance));
    }
}
