package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;

public class EntityAIMoveTowardsTarget extends GoalTask {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIMoveTowardsTarget(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e, float speed, float max) {
        this(mcc$creature(e), (double) speed, max);
    }

    public EntityAIMoveTowardsTarget(EntityCreature entity, double speed, float maxDistance) {
        super(entity, 1, m -> new MoveTowardsTargetGoal(m, speed, maxDistance));
    }
}
