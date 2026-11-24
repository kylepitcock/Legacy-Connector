package legacyconnector.v1_12_2.mezz.jei.plugins.vanilla.crafting;

/** JEI 4 (Minecraft 1.12.2) internals some plugins build on: the wrapper of a vanilla-style crafting recipe. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public class ShapedRecipesWrapper implements legacyconnector.v1_12_2.mezz.jei.api.recipe.wrapper.IShapedCraftingRecipeWrapper {
    protected final legacyconnector.v1_12_2.net.minecraft.item.crafting.ShapedRecipes recipe;

    public ShapedRecipesWrapper(legacyconnector.v1_12_2.mezz.jei.api.IJeiHelpers jeiHelpers, legacyconnector.v1_12_2.net.minecraft.item.crafting.ShapedRecipes recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients ingredients) {
        java.util.List<java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack>> inputs = new java.util.ArrayList<>();
        for (legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient i : recipe.getIngredients()) {
            java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> stacks = new java.util.ArrayList<>();
            if (i != null) for (legacyconnector.v1_12_2.net.minecraft.item.ItemStack s : i.getMatchingStacks()) if (s != null && !s.isEmpty()) stacks.add(s);
            inputs.add(stacks);
        }
        ingredients.setInputLists(legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class, inputs);
        ingredients.setOutput(legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class, recipe.getRecipeOutput());
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getRegistryName() {
        return recipe.getRegistryName();
    }

    @Override
    public int getWidth() {
        return recipe instanceof legacyconnector.v1_12_2.net.minecraftforge.common.crafting.IShapedRecipe s ? s.getRecipeWidth() : 3;
    }

    @Override
    public int getHeight() {
        return recipe instanceof legacyconnector.v1_12_2.net.minecraftforge.common.crafting.IShapedRecipe s ? s.getRecipeHeight() : 3;
    }
}
