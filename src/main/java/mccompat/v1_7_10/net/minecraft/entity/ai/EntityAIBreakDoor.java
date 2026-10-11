package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;

public class EntityAIBreakDoor extends GoalTask {
    public EntityAIBreakDoor(EntityLiving entity) {
        super(entity, 0, m -> new BreakDoorGoal(m, d -> d == Difficulty.HARD));
    }
}
