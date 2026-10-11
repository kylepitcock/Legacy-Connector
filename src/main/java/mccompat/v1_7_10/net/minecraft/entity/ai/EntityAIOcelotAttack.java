package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal;

public class EntityAIOcelotAttack extends GoalTask {
    public EntityAIOcelotAttack(EntityLiving entity) {
        super(entity, 3, OcelotAttackGoal::new);
    }
}
