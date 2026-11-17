package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.FurnaceRecipes;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/** 1.12.2 loot function: replaces the stack by its furnace result (same count); stacks with no smelting recipe stay as they are. */
public class Smelt extends LootFunction {
    public Smelt(LootCondition[] conditions) {
        super(conditions);
    }

    @Override
    public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
        if (stack.isEmpty()) return stack;
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);
        if (result == null || result.isEmpty()) return stack;
        ItemStack copy = result.copy();
        copy.setCount(stack.getCount());
        return copy;
    }
}
