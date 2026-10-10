package mccompat.v1_7_10.net.minecraft.item.crafting;

import mccompat.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.world.World;

public interface IRecipe {
    boolean matches(InventoryCrafting inv, World world);

    ItemStack getCraftingResult(InventoryCrafting inv);

    int getRecipeSize();

    ItemStack getRecipeOutput();
}
