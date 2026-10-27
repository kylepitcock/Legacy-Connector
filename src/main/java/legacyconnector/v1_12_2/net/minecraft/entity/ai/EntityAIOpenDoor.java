package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Opens (and optionally closes) doors on the path (the modern goal). */
public class EntityAIOpenDoor extends GoalTask1122 {
    public EntityAIOpenDoor(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, boolean closeDoor) {
        super(entity, 0, m -> new net.minecraft.world.entity.ai.goal.OpenDoorGoal(m, closeDoor));
    }

}
