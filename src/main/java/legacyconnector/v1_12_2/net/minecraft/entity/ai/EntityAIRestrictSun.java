package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Avoids the sun while pathing (the modern goal). */
public class EntityAIRestrictSun extends GoalTask1122 {
    public EntityAIRestrictSun(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature) {
        super(creature, 0, pathfinder(net.minecraft.world.entity.ai.goal.RestrictSunGoal::new));
    }

}
