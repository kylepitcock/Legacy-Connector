package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.RandomValueRange;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/** 1.12.2 loot function: damages a damageable stack to a random fraction of its durability. */
public class SetDamage extends LootFunction {
    private final RandomValueRange damageRange;

    public SetDamage(LootCondition[] conditions, RandomValueRange damageRange) {
        super(conditions);
        this.damageRange = damageRange;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
        if (stack.isItemStackDamageable()) {
            float f = 1.0F - damageRange.generateFloat(rand);
            stack.setItemDamage((int) Math.floor(f * stack.getMaxDamage()));
        }
        return stack;
    }
}
