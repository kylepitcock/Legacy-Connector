package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public class ShapelessRecipes extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    private final ItemStack recipeOutput;
    public final NonNullList<Ingredient> recipeItems;
    private final String group;

    public ShapelessRecipes(String group, ItemStack output, NonNullList<Ingredient> ingredients) {
        this.group = group;
        this.recipeOutput = output;
        this.recipeItems = ingredients;
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
        return width * height >= recipeItems.size();
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        return false;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return recipeOutput.copy();
    }
}
