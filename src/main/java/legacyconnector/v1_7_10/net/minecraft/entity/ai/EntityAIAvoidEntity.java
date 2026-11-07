package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;

public class EntityAIAvoidEntity extends GoalTask {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIAvoidEntity(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e, Class<?> cls, float dist, float far, float near) {
        this(mcc$creature(e), cls, dist, far, near);
    }

    /** 1.5.2 / 1.4.7 form with an EntityCreature owner: the speeds are absolute floats (see {@link GoalTask#mcc$multiplier}). */
    public EntityAIAvoidEntity(EntityCreature entity, Class<?> avoidClass, float distance, float farSpeed, float nearSpeed) {
        super(entity, 1, m -> new AvoidEntityGoal<LivingEntity>(m, LegacyTargets.search(avoidClass), LegacyTargets.of(avoidClass), distance,
                mcc$multiplier(m, farSpeed), mcc$multiplier(m, nearSpeed), e -> true));
    }

    public EntityAIAvoidEntity(EntityCreature entity, Class<?> avoidClass, float distance, double farSpeed, double nearSpeed) {
        super(entity, 1, m -> new AvoidEntityGoal<LivingEntity>(m, LegacyTargets.search(avoidClass), LegacyTargets.of(avoidClass), distance,
                farSpeed, nearSpeed, e -> true));
    }
}
