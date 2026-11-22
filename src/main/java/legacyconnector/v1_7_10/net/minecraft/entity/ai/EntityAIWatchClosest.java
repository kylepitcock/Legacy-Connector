package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;

public class EntityAIWatchClosest extends GoalTask {
    public EntityAIWatchClosest(EntityLiving entity, Class<?> watchTarget, float distance) {
        this(entity, watchTarget, distance, 0.02F);
    }

    public EntityAIWatchClosest(EntityLiving entity, Class<?> watchTarget, float distance, float chance) {
        super(entity, 2, m -> new LookAtPlayerGoal(m, LegacyTargets.search(watchTarget), distance, chance) {
            {
                // 1.7.10 found the entity to watch by distance alone (getClosestPlayerToEntity); the modern goal also needs a clear line of sight, so
                // a mob whose own block's collision box surrounds it (Witchery's mirror face, 0.4 in front of the mirror) never turned to the player
                lookAtContext.ignoreLineOfSight();
            }
        });
    }
}
