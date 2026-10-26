package legacyconnector.v1_12_2.net.minecraftforge.common.brewing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/**
 * Forge's brewing recipes. Mod recipes also become recipes of the modern brewing stand (see {@code Brewing1122}); vanilla's own are the
 * running game's.
 */
public class BrewingRecipeRegistry {
    private static final List<IBrewingRecipe> recipes = new ArrayList<>();

    static {
        addRecipe(new VanillaBrewingRecipe());
        org.kylepitcock.legacyconnector.v1_12_2.bridge.Brewing1122.ensureListener();
    }

    public static boolean addRecipe(ItemStack input, ItemStack ingredient, ItemStack output) {
        return addRecipe(new BrewingRecipe(input, ingredient, output));
    }

    public static boolean addRecipe(ItemStack input, String ingredient, ItemStack output) {
        return addRecipe(new BrewingOreRecipe(input, ingredient, output));
    }

    public static synchronized boolean addRecipe(IBrewingRecipe recipe) {
        return recipe != null && recipes.add(recipe);
    }

    public static synchronized ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (input.isEmpty() || input.getCount() != 1 || ingredient.isEmpty()) return ItemStack.EMPTY;
        for (IBrewingRecipe recipe : recipes) {
            ItemStack output = recipe.getOutput(input, ingredient);
            if (!output.isEmpty()) return output;
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasOutput(ItemStack input, ItemStack ingredient) {
        return !getOutput(input, ingredient).isEmpty();
    }

    public static boolean canBrew(NonNullList<ItemStack> inputs, ItemStack ingredient, int[] inputIndexes) {
        if (ingredient.isEmpty()) return false;
        for (int i : inputIndexes) if (hasOutput(inputs.get(i), ingredient)) return true;
        return false;
    }

    public static void brewPotions(NonNullList<ItemStack> inputs, ItemStack ingredient, int[] inputIndexes) {
        for (int i : inputIndexes) {
            ItemStack output = getOutput(inputs.get(i), ingredient);
            if (!output.isEmpty()) inputs.set(i, output);
        }
    }

    public static synchronized boolean isValidIngredient(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (IBrewingRecipe recipe : recipes) if (recipe.isIngredient(stack)) return true;
        return false;
    }

    public static synchronized boolean isValidInput(ItemStack stack) {
        if (stack.getCount() != 1) return false;
        for (IBrewingRecipe recipe : recipes) if (recipe.isInput(stack)) return true;
        return false;
    }

    public static synchronized List<IBrewingRecipe> getRecipes() {
        return Collections.unmodifiableList(new ArrayList<>(recipes));
    }
}
