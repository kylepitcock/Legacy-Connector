package mccompat.v1_7_10.net.minecraftforge.oredict;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.item.crafting.IRecipe;
import mccompat.v1_7_10.net.minecraft.world.World;

/**
 * Shaped recipe whose ingredients may be ore dictionary names. Never evaluated by the modern game: registering
 * it through GameRegistry translates the pattern into a data-driven {@code minecraft:crafting_shaped} recipe.
 */
public class ShapedOreRecipe implements IRecipe {
    private final ItemStack output;
    /** Raw legacy recipe arguments, exactly as passed to the constructor. */
    public final Object[] mcc$args;
    private boolean mirrored = true;

    public ShapedOreRecipe(Block result, Object... recipe) {
        this(new ItemStack(result), recipe);
    }

    public ShapedOreRecipe(Item result, Object... recipe) {
        this(new ItemStack(result), recipe);
    }

    public ShapedOreRecipe(ItemStack result, Object... recipe) {
        this.output = result.copy();
        Object[] args = recipe;
        if (args.length > 0 && args[0] instanceof Boolean b) {
            mirrored = b;
            Object[] rest = new Object[args.length - 1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            args = rest;
        }
        this.mcc$args = args;
    }

    public ShapedOreRecipe setMirrored(boolean mirror) {
        mirrored = mirror;
        return this;
    }

    public boolean mcc$mirrored() {
        return mirrored;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return output.copy();
    }

    @Override
    public int getRecipeSize() {
        return 9;
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
