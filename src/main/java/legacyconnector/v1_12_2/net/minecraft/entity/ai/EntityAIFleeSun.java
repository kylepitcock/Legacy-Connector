package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Finds shade in daylight (the modern goal). */
public class EntityAIFleeSun extends GoalTask1122 {
    public EntityAIFleeSun(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed) {
        super(creature, 1, pathfinder(m -> new net.minecraft.world.entity.ai.goal.FleeSunGoal(m, speed)));
    }

}
