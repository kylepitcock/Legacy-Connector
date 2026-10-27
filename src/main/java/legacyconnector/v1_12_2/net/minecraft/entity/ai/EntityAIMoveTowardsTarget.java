package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Walks towards the attack target while it is near (the modern goal). */
public class EntityAIMoveTowardsTarget extends GoalTask1122 {
    public EntityAIMoveTowardsTarget(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed, float targetMaxDistance) {
        super(creature, 1, pathfinder(m -> new net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal(m, speed, targetMaxDistance)));
    }

}
