package legacyconnector.vb1_7_3.net.minecraft.recipe;

import java.util.HashMap;
import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.vb1_7_3.RecipesB173;

/**
 * Beta 1.7.3 {@code FurnaceRecipes}: mods' furnaces and machines ({@code getSmeltingResult(itemId)}) see the recipes mods added (kept here
 * and turned into data pack recipes) and every smelting recipe of the modern game for the legacy item.
 */
public class SmeltingRecipeManager {
    public static final SmeltingRecipeManager INSTANCE = new SmeltingRecipeManager();
    /** Item id to result, the recipes b1.7.3 mods added. */
    public Map recipes = new HashMap();

    protected SmeltingRecipeManager() {}

    public static final SmeltingRecipeManager getInstance() {
        return INSTANCE;
    }

    public void addRecipe(int input, ItemStack output) {
        synchronized (recipes) {
            recipes.put(input, output);
        }
        RecipesB173.smelting(input, output);
    }

    /** MCP {@code getSmeltingResult}: the shared result stack (callers copy it), or null. */
    public ItemStack craft(int input) {
        Object own;
        synchronized (recipes) {
            own = recipes.get(input);
        }
        return own instanceof ItemStack s ? s : RecipesB173.smeltResult(input);
    }

    public Map getRecipes() {
        return recipes;
    }
}
