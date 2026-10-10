package mccompat.v1_7_10.net.minecraftforge.oredict;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.item.crafting.IRecipe;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Shapeless counterpart of {@link ShapedOreRecipe}; translated to {@code minecraft:crafting_shapeless}. */
public class ShapelessOreRecipe implements IRecipe {
    private final ItemStack output;
    public final Object[] mcc$args;

    public ShapelessOreRecipe(Block result, Object... recipe) {
        this(new ItemStack(result), recipe);
    }

    public ShapelessOreRecipe(Item result, Object... recipe) {
        this(new ItemStack(result), recipe);
    }

    public ShapelessOreRecipe(ItemStack result, Object... recipe) {
        this.output = result.copy();
        this.mcc$args = recipe;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return output.copy();
    }

    @Override
    public int getRecipeSize() {
        return mcc$args.length;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return output;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return false;
    }
}
