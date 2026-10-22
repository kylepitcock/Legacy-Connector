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

    // Forge's private field, read reflectively by recipe viewers.
    private final java.util.ArrayList<Object> input = new java.util.ArrayList<>();

    public ShapelessOreRecipe(Block result, Object... recipe) {
        this(new ItemStack(result), recipe);
    }

    public ShapelessOreRecipe(Item result, Object... recipe) {
        this(new ItemStack(result), recipe);
    }

    public ShapelessOreRecipe(ItemStack result, Object... recipe) {
        this.output = result.copy();
        this.mcc$args = recipe;
        for (Object o : recipe) {
            if (o instanceof mccompat.v1_7_10.net.minecraft.block.Block b) input.add(new ItemStack(b, 1, 32767));
            else if (o instanceof Item it) input.add(new ItemStack(it, 1, 32767));
            else input.add(o);
        }
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
