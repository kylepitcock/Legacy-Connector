package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;

public class EntityAIFleeSun extends GoalTask {
    /** 1.5.2 form: the owner is an EntityLiving, speeds are floats. */
    public EntityAIFleeSun(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving e, float speed) {
        this(mcc$creature(e), speed);
    }

    /** 1.5.2 / 1.4.7 form with an EntityCreature owner: the float is the absolute speed (see {@link GoalTask#mcc$multiplier}). */
    public EntityAIFleeSun(EntityCreature entity, float speed) {
        super(entity, 1, m -> new FleeSunGoal(m, mcc$multiplier(m, speed)));
    }

    public EntityAIFleeSun(EntityCreature entity, double speed) {
        super(entity, 1, m -> new FleeSunGoal(m, speed));
    }
}
