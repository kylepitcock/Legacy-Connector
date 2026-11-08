package legacyconnector.vb1_7_3.net.minecraft.recipe;

import java.util.ArrayList;
import java.util.List;

import legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingInventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.vb1_7_3.RecipesB173;

/**
 * Beta 1.7.3 {@code CraftingManager}. Recipes added here become data pack recipes like {@code ModLoader.AddRecipe}'s, and {@link #craft}
 * (a mod's own crafting table) asks the modern recipe manager, which holds vanilla's and the converted ones.
 */
public class CraftingRecipeManager {
    public static final CraftingRecipeManager INSTANCE = new CraftingRecipeManager();
    /** Stays empty: the recipes live in the modern recipe manager. */
    public List recipes = new ArrayList();

    protected CraftingRecipeManager() {}

    public static final CraftingRecipeManager getInstance() {
        return INSTANCE;
    }

    public void addShapedRecipe(ItemStack output, Object... inputs) {
        RecipesB173.shaped(output, inputs);
    }

    public void addShapelessRecipe(ItemStack output, Object... inputs) {
        RecipesB173.shapeless(output, inputs);
    }

    /** MCP {@code findMatchingRecipe}. */
    public ItemStack craft(CraftingInventory grid) {
        return RecipesB173.craft(grid);
    }

    public List getRecipes() {
        return recipes;
    }
}
