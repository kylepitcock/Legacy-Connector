package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;

public class EntityAIMoveTowardsRestriction extends GoalTask {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIMoveTowardsRestriction(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e, float speed) {
        this(mcc$creature(e), (double) speed);
    }

    public EntityAIMoveTowardsRestriction(EntityCreature entity, double speed) {
        super(entity, 1, m -> new MoveTowardsRestrictionGoal(m, speed));
    }
}
