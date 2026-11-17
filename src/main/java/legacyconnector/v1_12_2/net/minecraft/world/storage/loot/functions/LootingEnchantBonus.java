package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.RandomValueRange;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/** 1.12.2 loot function: adds a random amount per looting level to the stack count (capped by {@code limit} when it is not 0). */
public class LootingEnchantBonus extends LootFunction {
    private final RandomValueRange count;
    private final int limit;

    public LootingEnchantBonus(LootCondition[] conditions, RandomValueRange count, int limit) {
        super(conditions);
        this.count = count;
        this.limit = limit;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
        if (context.getKiller() instanceof EntityLivingBase) {
            int looting = context.getLootingModifier();
            if (looting == 0) return stack;
            float bonus = looting * count.generateFloat(rand);
            stack.grow(Math.round(bonus));
            if (limit != 0 && stack.getCount() > limit) stack.setCount(limit);
        }
        return stack;
    }
}
