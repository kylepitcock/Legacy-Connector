package legacyconnector.v1_12_2.net.minecraft.client.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook.RecipeList;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.CraftingManager;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_12_2.net.minecraft.stats.RecipeBook;

/**
 * 1.12.2's client recipe book table: the legacy crafting recipes grouped into recipe lists (by recipe group) and filed under the
 * creative tab of their result. The modern recipe book keeps its own table; this one serves legacy code that reads these lists.
 */
public class RecipeBookClient extends RecipeBook {
    public static final Map<CreativeTabs, List<RecipeList>> RECIPES_BY_TAB = new HashMap<>();
    public static final List<RecipeList> ALL_RECIPES = new ArrayList<>();

    static {
        rebuildTable();
    }

    /** Rebuilds the table from the recipe registry (mods call it after adding recipes at runtime). */
    public static synchronized void rebuildTable() {
        RECIPES_BY_TAB.clear();
        ALL_RECIPES.clear();
        Map<String, RecipeList> groups = new HashMap<>();
        try {
            for (IRecipe recipe : CraftingManager.REGISTRY) {
                if (recipe == null || recipe.isDynamic()) continue;
                CreativeTabs tab = getItemStackTab(recipe.getRecipeOutput());
                String group = recipe.getGroup();
                RecipeList list = group == null || group.isEmpty() ? null : groups.get(group);
                if (list == null) {
                    list = newRecipeList(tab);
                    if (group != null && !group.isEmpty()) groups.put(group, list);
                }
                list.add(recipe);
            }
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/RecipeBook").debug("Could not build the legacy recipe book table: {}", e.toString());
        }
    }

    private static CreativeTabs getItemStackTab(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return CreativeTabs.MISC;
        CreativeTabs tab = stack.getItem().getCreativeTab();
        if (tab == CreativeTabs.BUILDING_BLOCKS || tab == CreativeTabs.TOOLS || tab == CreativeTabs.REDSTONE) return tab;
        return tab == CreativeTabs.COMBAT ? CreativeTabs.TOOLS : CreativeTabs.MISC;
    }

    private static RecipeList newRecipeList(CreativeTabs tab) {
        RecipeList list = new RecipeList();
        ALL_RECIPES.add(list);
        RECIPES_BY_TAB.computeIfAbsent(tab, t -> new ArrayList<>()).add(list);
        RECIPES_BY_TAB.computeIfAbsent(CreativeTabs.SEARCH, t -> new ArrayList<>()).add(list);
        return list;
    }
}
