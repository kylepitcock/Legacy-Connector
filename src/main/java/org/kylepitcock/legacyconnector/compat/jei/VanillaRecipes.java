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

    /** The modern ingredient of the stacks of a legacy slot (the items; the 1.12 subtypes are items of their own). */
    private static net.minecraft.world.item.crafting.Ingredient ingredient(List<?> stacks) {
        List<net.minecraft.world.item.Item> items = new ArrayList<>();
        for (Object o : stacks) {
            if (o instanceof ItemStack s && !s.isEmpty()) {
                var item = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(s).getItem();
                if (item != net.minecraft.world.item.Items.AIR && !items.contains(item)) items.add(item);
            }
        }
        return items.isEmpty() ? net.minecraft.world.item.crafting.Ingredient.EMPTY : net.minecraft.world.item.crafting.Ingredient.of(items.toArray(new net.minecraft.world.item.Item[0]));
    }

    private static net.minecraft.resources.ResourceLocation idOf(IRecipeWrapper w, String kind, int n) {
        if (w instanceof legacyconnector.v1_12_2.mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper c) {
            try {
                var name = c.getRegistryName();
                if (name != null) {
                    String ns = name.getResourceDomain().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
                    String path = name.getResourcePath().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
                    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "jei1122/" + kind + "/" + ns + "/" + path);
                }
            } catch (RuntimeException | LinkageError e) {
                // numbered below
            }
        }
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "jei1122/" + kind + "/" + n);
    }

    /** A plugin's recipe in the vanilla crafting category as the modern crafting recipe JEI shows (shaped when the wrapper says so). */
    static net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> crafting(IRecipeWrapper w, int n) {
        var ing = new Jei1122Bridge.Ingredients();
        w.getIngredients(ing);
        List<List<?>> inputs = ing.inputs.getOrDefault(ItemStack.class, List.of());
        List<List<?>> outputs = ing.outputs.getOrDefault(ItemStack.class, List.of());
        if (outputs.isEmpty() || outputs.get(0).isEmpty() || !(outputs.get(0).get(0) instanceof ItemStack out) || out.isEmpty()) return null;
        var result = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(out);
        var list = net.minecraft.core.NonNullList.<net.minecraft.world.item.crafting.Ingredient>create();
        for (List<?> in : inputs) list.add(ingredient(in));
        net.minecraft.world.item.crafting.CraftingRecipe recipe;
        if (w instanceof legacyconnector.v1_12_2.mezz.jei.api.recipe.wrapper.IShapedCraftingRecipeWrapper shaped
                && shaped.getWidth() > 0 && shaped.getHeight() > 0 && shaped.getWidth() * shaped.getHeight() == list.size()) {
            recipe = new net.minecraft.world.item.crafting.ShapedRecipe("", net.minecraft.world.item.crafting.CraftingBookCategory.MISC,
                    new net.minecraft.world.item.crafting.ShapedRecipePattern(shaped.getWidth(), shaped.getHeight(), list, java.util.Optional.empty()), result);
        } else {
            list.removeIf(net.minecraft.world.item.crafting.Ingredient::isEmpty);
            if (list.isEmpty()) return null;
            recipe = new net.minecraft.world.item.crafting.ShapelessRecipe("", net.minecraft.world.item.crafting.CraftingBookCategory.MISC, result, list);
        }
        return new net.minecraft.world.item.crafting.RecipeHolder<>(idOf(w, "crafting", n), recipe);
    }

    /** A plugin's recipe in the vanilla smelting category: one input, one output. */
    static net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe> smelting(IRecipeWrapper w, int n) {
        var ing = new Jei1122Bridge.Ingredients();
        w.getIngredients(ing);
        List<List<?>> inputs = ing.inputs.getOrDefault(ItemStack.class, List.of());
        List<List<?>> outputs = ing.outputs.getOrDefault(ItemStack.class, List.of());
        if (inputs.isEmpty() || outputs.isEmpty() || outputs.get(0).isEmpty() || !(outputs.get(0).get(0) instanceof ItemStack out) || out.isEmpty()) return null;
        var input = ingredient(inputs.get(0));
        if (input.isEmpty()) return null;
        var recipe = new net.minecraft.world.item.crafting.SmeltingRecipe("", net.minecraft.world.item.crafting.CookingBookCategory.MISC, input,
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(out), 0.0F, 200);
        return new net.minecraft.world.item.crafting.RecipeHolder<>(idOf(w, "smelting", n), recipe);
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
