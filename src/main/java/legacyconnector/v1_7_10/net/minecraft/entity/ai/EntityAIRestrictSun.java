package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;

public class EntityAIRestrictSun extends GoalTask {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIRestrictSun(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e) {
        this(mcc$creature(e));
    }

    public EntityAIRestrictSun(EntityCreature entity) {
        super(entity, 0, RestrictSunGoal::new);
    }
}
