package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;

/** What earning an advancement gives (experience, loot, recipes, a function): a view of the modern rewards. */
public class AdvancementRewards {
    public static final AdvancementRewards EMPTY = new AdvancementRewards(net.minecraft.advancements.AdvancementRewards.EMPTY);
    private final net.minecraft.advancements.AdvancementRewards modern;

    public AdvancementRewards(net.minecraft.advancements.AdvancementRewards modern) {
        this.modern = modern == null ? net.minecraft.advancements.AdvancementRewards.EMPTY : modern;
    }

    /** 1.12's constructor: experience, loot tables, recipes and an optional function (Placebo builds rewards this way). */
    public AdvancementRewards(int experience, legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation[] loot,
            legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation[] recipes,
            legacyconnector.v1_12_2.net.minecraft.command.FunctionObject.CacheableFunction function) {
        this(mcc$build(experience, loot, recipes, function));
    }

    private static net.minecraft.advancements.AdvancementRewards mcc$build(int experience, legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation[] loot,
            legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation[] recipes,
            legacyconnector.v1_12_2.net.minecraft.command.FunctionObject.CacheableFunction function) {
        var lootKeys = new java.util.ArrayList<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>>();
        if (loot != null) for (var l : loot) lootKeys.add(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, l.mcc$modern()));
        var recipeIds = new java.util.ArrayList<net.minecraft.resources.ResourceLocation>();
        if (recipes != null) for (var r : recipes) recipeIds.add(r.mcc$modern());
        var fn = function == null || function.getId() == null ? java.util.Optional.<net.minecraft.commands.CacheableFunction>empty()
                : java.util.Optional.of(new net.minecraft.commands.CacheableFunction(function.getId().mcc$modern()));
        return new net.minecraft.advancements.AdvancementRewards(experience, lootKeys, recipeIds, fn);
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
