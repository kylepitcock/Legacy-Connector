package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Swims up in water and lava (the modern float goal). */
public class EntityAISwimming extends GoalTask1122 {
    public EntityAISwimming(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity) {
        super(entity, 4, net.minecraft.world.entity.ai.goal.FloatGoal::new);
        if (entity.getNavigator() != null) entity.getNavigator().setCanSwim(true);
    }

}
