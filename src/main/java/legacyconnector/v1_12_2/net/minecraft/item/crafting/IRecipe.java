package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public interface IRecipe extends IForgeRegistryEntry<IRecipe> {
    boolean matches(InventoryCrafting inv, World worldIn);

    ItemStack getCraftingResult(InventoryCrafting inv);

    boolean canFit(int width, int height);

    ItemStack getRecipeOutput();

    default NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return legacyconnector.v1_12_2.net.minecraftforge.common.ForgeHooks.defaultRecipeGetRemainingItems(inv);
    }

    default NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    default boolean isDynamic() {
        return false;
    }

    default String getGroup() {
        return "";
    }
}
