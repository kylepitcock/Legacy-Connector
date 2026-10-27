package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Looks around at random (the modern random look-around goal). */
public class EntityAILookIdle extends GoalTask1122 {
    public EntityAILookIdle(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity) {
        super(entity, 3, net.minecraft.world.entity.ai.goal.RandomLookAroundGoal::new);
    }

}
