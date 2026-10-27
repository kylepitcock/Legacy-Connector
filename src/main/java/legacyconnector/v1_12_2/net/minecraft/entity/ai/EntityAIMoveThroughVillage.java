package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Walks through the village (the modern goal, without door handling). */
public class EntityAIMoveThroughVillage extends GoalTask1122 {
    public EntityAIMoveThroughVillage(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed, boolean onlyAtNight) {
        super(creature, 1, pathfinder(m -> new net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal(m, speed, onlyAtNight, 4, () -> false)));
    }

}
