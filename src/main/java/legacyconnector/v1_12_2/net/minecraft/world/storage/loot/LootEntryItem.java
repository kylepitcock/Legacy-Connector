package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import java.util.Collection;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootConditionManager;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.LootFunction;

/** 1.12.2 loot entry of one item, shaped by its functions (count, metadata, NBT...) and split into full stacks. */
public class LootEntryItem extends LootEntry {
    protected final Item item;
    protected final LootFunction[] functions;

    public LootEntryItem(Item item, int weight, int quality, LootFunction[] functions, LootCondition[] conditions, String entryName) {
        super(weight, quality, conditions, entryName);
        this.item = item;
        this.functions = functions == null ? new LootFunction[0] : functions;
    }

    @Override
    public void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
        if (item == null) return;
        ItemStack stack = new ItemStack(item);
        for (LootFunction f : functions) {
            if (LootConditionManager.testAllConditions(f.getConditions(), rand, context)) stack = f.apply(stack, rand, context);
        }
        if (stack == null || stack.isEmpty()) return;
        int max = Math.max(1, stack.getMaxStackSize());
        if (stack.getCount() < max) {
            stacks.add(stack);
            return;
        }
        int count = stack.getCount();
        while (count > 0) {
            ItemStack part = stack.copy();
            part.setCount(Math.min(max, count));
            count -= part.getCount();
            stacks.add(part);
        }
    }
}
