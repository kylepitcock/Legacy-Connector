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
        this.matchingStacks = stacks == null ? new ItemStack[0] : stacks;
    }

    /** Forge's constructor for subclasses that compute their own matches (they override getMatchingStacks/apply). */
    protected Ingredient(int size) {
        this(new ItemStack[0]);
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

    /** The matching stacks packed as recipe-helper ids (item id and metadata; wildcards expanded to the item's subtypes), sorted. */
    public it.unimi.dsi.fastutil.ints.IntList getValidItemStacksPacked() {
        it.unimi.dsi.fastutil.ints.IntArrayList packed = new it.unimi.dsi.fastutil.ints.IntArrayList();
        for (ItemStack stack : getMatchingStacks()) {
            if (stack == null || stack.isEmpty()) continue;
            if (stack.getMetadata() == 32767) {
                legacyconnector.v1_12_2.net.minecraft.util.NonNullList<ItemStack> subs = legacyconnector.v1_12_2.net.minecraft.util.NonNullList.create();
                try {
                    stack.getItem().getSubItems(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.SEARCH, subs);
                } catch (RuntimeException | LinkageError e) {
                    // the item lists no subtypes outside the client
                }
                if (subs.isEmpty()) subs.add(new ItemStack(stack.getItem()));
                for (ItemStack sub : subs) packed.add(legacyconnector.v1_12_2.net.minecraft.client.util.RecipeItemHelper.pack(sub));
            } else {
                packed.add(legacyconnector.v1_12_2.net.minecraft.client.util.RecipeItemHelper.pack(stack));
            }
        }
        packed.sort(it.unimi.dsi.fastutil.ints.IntComparators.NATURAL_COMPARATOR);
        return packed;
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

    /** One ingredient matching what any of the given ingredients matches. */
    public static Ingredient merge(java.util.Collection<Ingredient> parts) {
        return new legacyconnector.v1_12_2.net.minecraftforge.common.crafting.CompoundIngredient(parts);
    }
}
