package legacyconnector.v1_12_2.net.minecraft.client.util;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;

/**
 * The recipe book's count of available items (packed as item id and damage): which recipes the counted items can make. Ingredients are
 * matched greedily, each to the first counted item it accepts.
 */
public class RecipeItemHelper {
    public final Int2IntMap itemToCount = new Int2IntOpenHashMap();

    public RecipeItemHelper() {
    }

    public void accountStack(ItemStack stack) {
        accountStack(stack, -1);
    }

    /** Counts a stack (undamaged, unenchanted, unnamed ones only, as 1.12), at most {@code forceCount} of it when that is positive. */
    public void accountStack(ItemStack stack, int forceCount) {
        if (stack.isEmpty() || stack.isItemDamaged() || stack.isItemEnchanted() || stack.hasDisplayName()) return;
        int packed = pack(stack);
        int count = forceCount > 0 ? Math.min(forceCount, stack.getCount()) : stack.getCount();
        itemToCount.put(packed, itemToCount.get(packed) + count);
    }

    public static int pack(ItemStack stack) {
        Item item = stack.getItem();
        int damage = item.getHasSubtypes() ? stack.getMetadata() : 0;
        return Item.getIdFromItem(item) << 16 | damage & 0xFFFF;
    }

    public static ItemStack unpack(int packed) {
        return packed == 0 ? ItemStack.EMPTY : new ItemStack(Item.getItemById(packed >> 16 & 0xFFFF), 1, packed & 0xFFFF);
    }

    public boolean containsItem(int packed) {
        return itemToCount.get(packed) > 0;
    }

    public void clear() {
        itemToCount.clear();
    }

    public boolean canCraft(IRecipe recipe, IntList packedItemList) {
        return getBiggestCraftableStack(recipe, 1, packedItemList) > 0;
    }

    public boolean canCraft(IRecipe recipe, IntList packedItemList, int maxAmount) {
        return getBiggestCraftableStack(recipe, maxAmount, packedItemList) >= maxAmount;
    }

    public int getBiggestCraftableStack(IRecipe recipe, IntList packedItemList) {
        return getBiggestCraftableStack(recipe, Integer.MAX_VALUE, packedItemList);
    }

    /** How many times (up to maxAmount) the counted items make the recipe; the items used once go into {@code packedItemList}. */
    public int getBiggestCraftableStack(IRecipe recipe, int maxAmount, IntList packedItemList) {
        var ingredients = recipe.getIngredients();
        Int2IntOpenHashMap left = new Int2IntOpenHashMap(itemToCount);
        int made = 0;
        while (made < maxAmount) {
            Int2IntOpenHashMap trial = new Int2IntOpenHashMap(left);
            IntList used = new it.unimi.dsi.fastutil.ints.IntArrayList();
            boolean ok = true;
            for (Ingredient ingredient : ingredients) {
                if (ingredient == null || ingredient == Ingredient.EMPTY) continue;
                int found = 0;
                for (var e : trial.int2IntEntrySet()) {
                    if (e.getIntValue() > 0 && ingredient.apply(unpack(e.getIntKey()))) {
                        found = e.getIntKey();
                        break;
                    }
                }
                if (found == 0) {
                    ok = false;
                    break;
                }
                trial.put(found, trial.get(found) - 1);
                used.add(found);
            }
            if (!ok) break;
            if (made == 0 && packedItemList != null) {
                packedItemList.clear();
                packedItemList.addAll(used);
            }
            left = trial;
            made++;
        }
        return made;
    }
}
