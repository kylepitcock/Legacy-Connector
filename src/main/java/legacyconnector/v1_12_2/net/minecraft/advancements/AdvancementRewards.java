package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;

/** What earning an advancement gives (experience, loot, recipes, a function): a view of the modern rewards. */
public class AdvancementRewards {
    public static final AdvancementRewards EMPTY = new AdvancementRewards(net.minecraft.advancements.AdvancementRewards.EMPTY);
    private final net.minecraft.advancements.AdvancementRewards modern;

    public AdvancementRewards(net.minecraft.advancements.AdvancementRewards modern) {
        this.modern = modern == null ? net.minecraft.advancements.AdvancementRewards.EMPTY : modern;
    }

    public net.minecraft.advancements.AdvancementRewards mcc$modern() {
        return modern;
    }

    /** Gives the rewards to the player. */
    public void apply(EntityPlayerMP player) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp != null) modern.grant(sp);
    }
}
