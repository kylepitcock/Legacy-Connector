package legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.util.RecipeItemHelper;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_12_2.net.minecraft.stats.RecipeBook;

/** 1.12.2 recipe book entry: recipes grouped under one button, with which of them are known, fit the grid and can be crafted now. */
public class RecipeList {
    private final List<IRecipe> recipes = new ArrayList<>();
    private final BitSet craftable = new BitSet();
    private final BitSet canFit = new BitSet();
    private final BitSet inBook = new BitSet();
    private boolean singleResultItem = true;

    public boolean isNotEmpty() {
        return !inBook.isEmpty();
    }

    public void updateKnownRecipes(RecipeBook book) {
        for (int i = 0; i < recipes.size(); ++i) inBook.set(i, book.isUnlocked(recipes.get(i)));
    }

    public void canCraft(RecipeItemHelper handler, int width, int height, RecipeBook book) {
        for (int i = 0; i < recipes.size(); ++i) {
            IRecipe recipe = recipes.get(i);
            boolean fits = recipe.canFit(width, height) && book.isUnlocked(recipe);
            canFit.set(i, fits);
            craftable.set(i, fits && handler.canCraft(recipe, null));
        }
    }

    public boolean isCraftable(IRecipe recipe) {
        int i = recipes.indexOf(recipe);
        return i >= 0 && craftable.get(i);
    }

    public boolean containsCraftableRecipes() {
        return !craftable.isEmpty();
    }

    public boolean containsValidRecipes() {
        return !canFit.isEmpty();
    }

    public List<IRecipe> getRecipes() {
        return recipes;
    }

    public List<IRecipe> getRecipes(boolean onlyCraftable) {
        List<IRecipe> list = new ArrayList<>();
        BitSet set = onlyCraftable ? craftable : canFit;
        for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) list.add(recipes.get(i));
        return list;
    }

    public List<IRecipe> getDisplayRecipes(boolean craftableOnly) {
        List<IRecipe> list = new ArrayList<>();
        for (int i = 0; i < recipes.size(); ++i) {
            if (canFit.get(i) && craftable.get(i) == craftableOnly) list.add(recipes.get(i));
        }
        return list;
    }

    public void add(IRecipe recipe) {
        recipes.add(recipe);
        if (singleResultItem) {
            ItemStack a = recipes.get(0).getRecipeOutput();
            ItemStack b = recipe.getRecipeOutput();
            singleResultItem = ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
        }
    }

    public boolean hasSingleResultItem() {
        return singleResultItem;
    }
}
