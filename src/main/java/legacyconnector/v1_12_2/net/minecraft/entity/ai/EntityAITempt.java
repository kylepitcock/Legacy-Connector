package legacyconnector.v1_12_2.net.minecraft.entity.ai;

/** Follows a player holding a tempting item (the modern tempt goal). */
public class EntityAITempt extends GoalTask1122 {
    private final java.util.Set<legacyconnector.v1_12_2.net.minecraft.item.Item> temptItem;
    /** Phantom: the player the modern tempt goal follows. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer temptingPlayer;

    private static final java.lang.reflect.Field MCC$PLAYER = mcc$playerField();

    private static java.lang.reflect.Field mcc$playerField() {
        try {
            return net.neoforged.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.world.entity.ai.goal.TemptGoal.class, "player");
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer mcc$get$temptingPlayer(EntityAITempt t) {
        try {
            Object p = MCC$PLAYER == null || !(t.mcc$goal() instanceof net.minecraft.world.entity.ai.goal.TemptGoal g) ? null : MCC$PLAYER.get(g);
            return p instanceof net.minecraft.world.entity.player.Player player
                    && org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(player) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer ep
                    ? ep : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static void mcc$set$temptingPlayer(EntityAITempt t, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer v) {
        if (MCC$PLAYER == null || !(t.mcc$goal() instanceof net.minecraft.world.entity.ai.goal.TemptGoal g)) return;
        try {
            MCC$PLAYER.set(g, v == null ? null : v.mcc$player());
        } catch (IllegalAccessException e) {
            // keeps the goal's player
        }
    }

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
