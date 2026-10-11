package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;

public class EntityAIOpenDoor extends GoalTask {
    public EntityAIOpenDoor(EntityLiving entity, boolean closeDoor) {
        super(entity, 0, m -> new OpenDoorGoal(m, closeDoor));
        if (entity.getNavigator() != null) entity.getNavigator().setEnterDoors(true);
    }
}
