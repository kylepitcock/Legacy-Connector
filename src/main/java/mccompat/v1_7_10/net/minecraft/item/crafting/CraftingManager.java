package mccompat.v1_7_10.net.minecraft.item.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.mccompat.v1_7_10.bridge.RecipeBridge;

/**
 * The legacy crafting registry. Its list holds the recipes legacy mods registered (vanilla recipes live in the modern
 * recipe manager); anything added to it - including {@code getRecipeList().add(...)} - becomes a modern recipe.
 */
public class CraftingManager {
    private static final CraftingManager instance = new CraftingManager();
    @SuppressWarnings("rawtypes")
    private List recipes = new RecipeList();

    public static CraftingManager getInstance() {
        return instance;
    }

    public ShapedRecipes addRecipe(ItemStack output, Object... params) {
        StringBuilder pattern = new StringBuilder();
        int i = 0;
        int width = 0;
        int height = 0;
        if (params[i] instanceof String[] rows) {
            i++;
            for (String row : rows) {
                height++;
                width = row.length();
                pattern.append(row);
            }
        } else {
            while (params[i] instanceof String row) {
                i++;
                height++;
                width = row.length();
                pattern.append(row);
            }
        }
        java.util.Map<Character, ItemStack> key = new java.util.HashMap<>();
        for (; i + 1 < params.length; i += 2) {
            Object v = params[i + 1];
            ItemStack s = v instanceof Item it ? new ItemStack(it) : v instanceof Block b ? new ItemStack(b, 1, 32767) : v instanceof ItemStack st ? st : null;
            key.put((Character) params[i], s);
        }
        ItemStack[] items = new ItemStack[width * height];
        for (int k = 0; k < items.length; k++) {
            ItemStack s = key.get(pattern.charAt(k));
            items[k] = s == null ? null : s.copy();
        }
        ShapedRecipes recipe = new ShapedRecipes(width, height, items, output);
        recipes.add(recipe);
        return recipe;
    }

    public void addShapelessRecipe(ItemStack output, Object... params) {
        List<ItemStack> items = new ArrayList<>();
        for (Object o : params) {
            if (o instanceof ItemStack s) items.add(s.copy());
            else if (o instanceof Item it) items.add(new ItemStack(it));
            else if (o instanceof Block b) items.add(new ItemStack(b));
        }
        recipes.add(new ShapelessRecipes(output, items));
    }

    public ItemStack findMatchingRecipe(InventoryCrafting inv, World world) {
        for (Object o : recipes) {
            IRecipe r = (IRecipe) o;
            if (r.matches(inv, world)) return r.getCraftingResult(inv);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public List getRecipeList() {
        return recipes;
    }

    /** Lists a recipe the bridge already translated (GameRegistry paths), without translating it again. */
    public void mcc$track(IRecipe recipe) {
        // A mod may have swapped the list for its own (DragonAPI's EventRecipeList): then the recipe is only listed.
        if (recipes instanceof RecipeList list) list.addUntracked(recipe);
        else recipes.add(recipe);
    }

    /** Forwards additions to the recipe bridge (mods often append to the list directly). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class RecipeList extends ArrayList {
        void addUntracked(Object o) {
            super.add(o);
        }

        @Override
        public boolean add(Object o) {
            if (o instanceof IRecipe r) RecipeBridge.addLegacyRecipe(r);
            return super.add(o);
        }

        @Override
        public void add(int index, Object o) {
            if (o instanceof IRecipe r) RecipeBridge.addLegacyRecipe(r);
            super.add(index, o);
        }

        @Override
        public boolean addAll(Collection c) {
            for (Object o : c) if (o instanceof IRecipe r) RecipeBridge.addLegacyRecipe(r);
            return super.addAll(c);
        }
    }
}
