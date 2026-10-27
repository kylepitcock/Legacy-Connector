package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Follows a player holding a tempting item (the modern tempt goal). */
public class EntityAITempt extends GoalTask1122 {
    private final java.util.Set<legacyconnector.v1_12_2.net.minecraft.item.Item> temptItem;

    public EntityAITempt(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed, legacyconnector.v1_12_2.net.minecraft.item.Item item, boolean scaredByPlayerMovement) {
        this(creature, speed, scaredByPlayerMovement, com.google.common.collect.Sets.newHashSet(item));
    }

    public EntityAITempt(legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature creature, double speed, boolean scaredByPlayerMovement, java.util.Set<legacyconnector.v1_12_2.net.minecraft.item.Item> items) {
        super(creature, 3, pathfinder(m -> new net.minecraft.world.entity.ai.goal.TemptGoal(m, speed, s -> items.contains(
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(s).getItem()), scaredByPlayerMovement)));
        this.temptItem = items;
    }

    protected boolean isTempting(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        return temptItem.contains(stack.getItem());
    }

    public boolean isRunning() {
        return mcc$goal() instanceof net.minecraft.world.entity.ai.goal.TemptGoal g && g.isRunning();
    }

}
