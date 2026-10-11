package mccompat.v1_7_10.net.minecraft.entity.ai;

import mccompat.v1_7_10.net.minecraft.entity.EntityCreature;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.entity.ai.goal.TemptGoal;

public class EntityAITempt extends GoalTask {
    public EntityAITempt(EntityCreature entity, double speed, Item item, boolean scaredByPlayerMovement) {
        super(entity, 3, m -> new TemptGoal(m, speed, s -> item != null && ItemStack.mcc$wrap(s) != null && ItemStack.mcc$wrap(s).getItem() == item,
                scaredByPlayerMovement));
    }
}
