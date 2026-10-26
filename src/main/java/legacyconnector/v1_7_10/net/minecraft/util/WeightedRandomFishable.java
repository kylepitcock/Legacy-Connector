package legacyconnector.v1_7_10.net.minecraft.util;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** One entry of the fishing loot lists (1.7.10). */
public class WeightedRandomFishable extends WeightedRandom.Item {
    private final ItemStack returnStack;
    private float maxDamagePercent;
    private boolean enchantable;

    public WeightedRandomFishable(ItemStack stack, int weight) {
        super(weight);
        this.returnStack = stack;
    }

    /** A copy of the entry's stack, randomly worn down to at most {@code maxDamagePercent} and enchanted if enchantable. */
    public ItemStack getItemStack(Random random) {
        ItemStack stack = returnStack.copy();
        if (maxDamagePercent > 0.0F) {
            int max = (int) (maxDamagePercent * returnStack.getMaxDamage());
            int damage = stack.getMaxDamage() - random.nextInt(random.nextInt(Math.max(max, 1)) + 1);
            if (damage > max) damage = max;
            if (damage < 1) damage = 1;
            stack.setItemDamage(damage);
        }
        if (enchantable) EnchantmentHelper.addRandomEnchantment(random, stack, 30);
        return stack;
    }

    public WeightedRandomFishable setMaxDamagePercent(float maxDamagePercent) {
        this.maxDamagePercent = maxDamagePercent;
        return this;
    }

    public WeightedRandomFishable setEnchantable() {
        this.enchantable = true;
        return this;
    }
}
