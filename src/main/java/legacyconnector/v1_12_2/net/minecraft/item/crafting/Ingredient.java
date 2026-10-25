package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import com.google.common.base.Predicate;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** One slot of a recipe: the stacks that fit it. */
public class Ingredient implements Predicate<ItemStack> {
    public static final Ingredient EMPTY = new Ingredient(new ItemStack[0]);

    private final ItemStack[] matchingStacks;
    /** What the ingredient was made from (stacks, or an ore dictionary name), for translating recipes. */
    public Object mcc$source;

    protected Ingredient(ItemStack... stacks) {
        this.matchingStacks = stacks;
    }

    public ItemStack[] getMatchingStacks() {
        return matchingStacks;
    }

    @Override
    public boolean apply(ItemStack input) {
        if (input == null) return false;
        for (ItemStack s : matchingStacks) {
            if (s.getItem() == input.getItem()) {
                int meta = s.getMetadata();
                if (meta == 32767 || meta == input.getMetadata()) return true;
            }
        }
        return false;
    }

    public boolean isSimple() {
        return true;
    }

    public static Ingredient fromItem(Item item) {
        return fromStacks(new ItemStack(item, 1, 32767));
    }

    public static Ingredient fromItems(Item... items) {
        ItemStack[] stacks = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) stacks[i] = new ItemStack(items[i], 1, 32767);
        return fromStacks(stacks);
    }

    public static Ingredient fromStacks(ItemStack... stacks) {
        if (stacks.length == 0) return EMPTY;
        Ingredient ing = new Ingredient(stacks);
        ing.mcc$source = stacks;
        return ing;
    }

    public static Ingredient fromBlock(Block block) {
        return fromStacks(new ItemStack(block, 1, 32767));
    }
}
