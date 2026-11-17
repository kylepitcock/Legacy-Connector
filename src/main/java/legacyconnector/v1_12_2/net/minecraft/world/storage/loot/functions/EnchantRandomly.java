package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentData;
import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.ItemEnchantedBook;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/**
 * 1.12.2 loot function: puts one random enchantment, at a random level of its range, on the stack (a book becomes an enchanted book).
 * With no list every registered enchantment that can apply to the stack qualifies.
 */
public class EnchantRandomly extends LootFunction {
    private final List<Enchantment> enchantments;

    public EnchantRandomly(LootCondition[] conditions, List<Enchantment> enchantments) {
        super(conditions);
        this.enchantments = enchantments == null ? List.of() : enchantments;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
        Enchantment chosen;
        if (!enchantments.isEmpty()) {
            chosen = enchantments.get(rand.nextInt(enchantments.size()));
        } else {
            List<Enchantment> candidates = new ArrayList<>();
            for (Enchantment e : Enchantment.REGISTRY) {
                if (stack.getItem() == Items.BOOK || e.canApply(stack)) candidates.add(e);
            }
            if (candidates.isEmpty()) return stack;
            chosen = candidates.get(rand.nextInt(candidates.size()));
        }
        int level = MathHelper.getInt(rand, chosen.getMinLevel(), chosen.getMaxLevel());
        if (stack.getItem() == Items.BOOK) {
            stack = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantedBook.addEnchantment(stack, new EnchantmentData(chosen, level));
        } else {
            stack.addEnchantment(chosen, level);
        }
        return stack;
    }
}
