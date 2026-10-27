package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Breaks wooden doors on hard difficulty (the modern goal). */
public class EntityAIBreakDoor extends GoalTask1122 {
    public EntityAIBreakDoor(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity) {
        super(entity, 0, m -> new net.minecraft.world.entity.ai.goal.BreakDoorGoal(m, d -> d == net.minecraft.world.Difficulty.HARD));
    }

}
