package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.RandomValueRange;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/** 1.12.2 loot function: sets the stack size to a random value of the range. */
public class SetCount extends LootFunction {
    private final RandomValueRange countRange;

    public SetCount(LootCondition[] conditions, RandomValueRange countRange) {
        super(conditions);
        this.countRange = countRange;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
        stack.setCount(countRange.generateInt(rand));
        return stack;
    }
}
