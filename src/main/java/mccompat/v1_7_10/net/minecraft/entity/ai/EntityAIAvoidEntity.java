package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;

public class EntityAIAvoidEntity extends GoalTask {
    public EntityAIAvoidEntity(EntityCreature entity, Class<?> avoidClass, float distance, double farSpeed, double nearSpeed) {
        super(entity, 1, m -> new AvoidEntityGoal<LivingEntity>(m, LegacyTargets.search(avoidClass), LegacyTargets.of(avoidClass), distance,
                farSpeed, nearSpeed, e -> true));
    }
}
