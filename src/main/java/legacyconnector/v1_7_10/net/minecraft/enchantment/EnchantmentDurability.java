package legacyconnector.v1_7_10.net.minecraft.enchantment;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.item.ItemArmor;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Unbreaking ({@code durability}), ported from 1.7.10: each level makes a use less likely to cost durability. */
public class EnchantmentDurability extends Enchantment {
    protected EnchantmentDurability(int id, int weight) {
        super(id, weight, EnumEnchantmentType.breakable);
        setName("durability");
    }

    @Override
    public int getMinEnchantability(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return super.getMinEnchantability(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canApply(ItemStack stack) {
        return stack.isItemStackDamageable() || super.canApply(stack);
    }

    /**
     * Whether one point of damage to the stack is cancelled by unbreaking of that level (armour only keeps the chance part:
     * 60% of its hits always cost durability).
     */
    public static boolean negateDamage(ItemStack stack, int level, Random random) {
        if (stack != null && stack.getItem() instanceof ItemArmor && random.nextFloat() < 0.6F) return false;
        return random.nextInt(level + 1) > 0;
    }
}
