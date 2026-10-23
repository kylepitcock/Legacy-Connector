package legacyconnector.v1_7_10.net.minecraft.util;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** One entry of the fishing loot lists (1.7.10). */
public class WeightedRandomFishable extends WeightedRandom.Item {
    private final ItemStack field_150711_b;
    private float field_150712_c = 1.0F;
    private boolean field_150710_d;
    private boolean field_150713_e;

    public WeightedRandomFishable(ItemStack stack, int weight) {
        super(weight);
        this.field_150711_b = stack;
    }

    public ItemStack func_150708_a(Random random) {
        ItemStack stack = field_150711_b.copy();
        if (field_150712_c > 0.0F) {
            int max = (int) (field_150712_c * stack.getMaxDamage());
            int damage = stack.getMaxDamage() - random.nextInt(random.nextInt(Math.max(max, 1)) + 1);
            if (damage > max) damage = max;
            if (damage < 1) damage = 1;
            stack.setItemDamage(damage);
        }
        return stack;
    }

    public WeightedRandomFishable func_150709_a(float maxDamagePercent) {
        this.field_150712_c = maxDamagePercent;
        return this;
    }

    public WeightedRandomFishable func_150707_a() {
        this.field_150710_d = true;
        return this;
    }

    public WeightedRandomFishable func_150705_b() {
        this.field_150713_e = true;
        return this;
    }
}
