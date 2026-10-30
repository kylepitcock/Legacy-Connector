package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.RandomValueRange;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/**
 * 1.12.2 loot function: enchants the stack as an enchanting table at a random level would (books become enchanted books). The
 * modern enchanting rules pick the enchantments: the enchanting table's set, or every enchantment that may appear on random loot
 * when treasure is allowed.
 */
public class EnchantWithLevels extends LootFunction {
    private final RandomValueRange randomLevel;
    private final boolean isTreasure;

    public EnchantWithLevels(LootCondition[] conditions, RandomValueRange randomRange, boolean isTreasure) {
        super(conditions);
        this.randomLevel = randomRange;
        this.isTreasure = isTreasure;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (stack == null || server == null) return stack;
        try {
            var access = server.registryAccess();
            var tag = isTreasure ? net.minecraft.tags.EnchantmentTags.ON_RANDOM_LOOT : net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE;
            var options = access.registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getTag(tag);
            var random = net.minecraft.util.RandomSource.create(rand == null ? 0L : rand.nextLong());
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
            var enchanted = net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(random, modern, randomLevel.generateInt(rand), access, options);
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(enchanted);
        } catch (RuntimeException e) {
            return stack;
        }
    }
}
