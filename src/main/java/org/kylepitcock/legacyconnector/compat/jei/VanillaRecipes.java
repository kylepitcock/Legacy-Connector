package org.kylepitcock.legacyconnector.compat.jei;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IVanillaRecipeFactory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** JEI 1.12's vanilla recipe factory: wrappers a legacy plugin adds to the anvil or brewing category, turned into the modern recipes on registration. */
final class VanillaRecipes {
    private VanillaRecipes() {}

    record Anvil(ItemStack left, List<ItemStack> rights, List<ItemStack> outputs) implements IRecipeWrapper {
        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInputLists(ItemStack.class, List.of(single(left), rights));
            ingredients.setOutputs(ItemStack.class, outputs);
        }
    }

    record Brewing(List<ItemStack> ingredients, ItemStack input, ItemStack output) implements IRecipeWrapper {
        @Override
        public void getIngredients(IIngredients in) {
            in.setInputLists(ItemStack.class, List.of(single(input), ingredients));
            in.setOutput(ItemStack.class, output);
        }
    }

    private static List<ItemStack> single(ItemStack s) {
        List<ItemStack> l = new ArrayList<>();
        if (s != null) l.add(s);
        return l;
    }

    static final IVanillaRecipeFactory FACTORY = new IVanillaRecipeFactory() {
        @Override
        public IRecipeWrapper createAnvilRecipe(ItemStack left, List<ItemStack> rights, List<ItemStack> outputs) {
            return new Anvil(left, rights == null ? List.of() : List.copyOf(rights), outputs == null ? List.of() : List.copyOf(outputs));
        }

        @Override
        public IRecipeWrapper createBrewingRecipe(List<ItemStack> ingredients, ItemStack input, ItemStack output) {
            return new Brewing(ingredients == null ? List.of() : List.copyOf(ingredients), input, output);
        }
    };
}
