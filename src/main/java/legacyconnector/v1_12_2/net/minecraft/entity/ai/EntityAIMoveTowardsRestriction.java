package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Walks back into the home area (the modern goal). */
public class EntityAIMoveTowardsRestriction extends GoalTask1122 {
    public EntityAIMoveTowardsRestriction(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed) {
        super(creature, 1, pathfinder(m -> new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(m, speed)));
    }

}
