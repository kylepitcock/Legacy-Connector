package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;

public class EntityAIWander extends GoalTask {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIWander(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e, float speed) {
        this(mcc$creature(e), (double) speed);
    }

    public EntityAIWander(EntityCreature entity, double speed) {
        super(entity, 1, m -> new RandomStrollGoal(m, speed));
    }
}
