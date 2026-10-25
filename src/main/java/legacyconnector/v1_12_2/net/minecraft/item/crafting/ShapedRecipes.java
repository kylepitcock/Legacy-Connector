package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public class ShapedRecipes extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    public final int recipeWidth;
    public final int recipeHeight;
    public final NonNullList<Ingredient> recipeItems;
    protected final ItemStack recipeOutput;
    protected final String group;

    public ShapedRecipes(String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
        this.group = group;
        this.recipeWidth = width;
        this.recipeHeight = height;
        this.recipeItems = ingredients;
        this.recipeOutput = result;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return recipeOutput;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width >= recipeWidth && height >= recipeHeight;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        return false;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return getRecipeOutput().copy();
    }

    public int getWidth() {
        return recipeWidth;
    }

    public int getHeight() {
        return recipeHeight;
    }
}
