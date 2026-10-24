package legacyconnector.v1_7_10.net.minecraftforge.oredict;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Shaped recipe whose ingredients may be ore dictionary names. Never evaluated by the modern game: registering
 * it through GameRegistry translates the pattern into a data-driven {@code minecraft:crafting_shaped} recipe.
 */
public class ShapedOreRecipe implements IRecipe {
    private final ItemStack output;
    /** Raw legacy recipe arguments, exactly as passed to the constructor. */
    public final Object[] mcc$args;
    private boolean mirrored = true;
    // Forge's private layout, read reflectively by recipe viewers (Guide-API, NEI).
    private Object[] input;
    private int width;
    private int height;

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
        parseLayout(args);
    }

    private void parseLayout(Object[] args) {
        int i = 0;
        java.util.List<String> rows = new java.util.ArrayList<>();
        if (args.length > 0 && args[0] instanceof Object[] arr) {
            for (Object o : arr) rows.add(String.valueOf(o));
            i = 1;
        } else {
            while (i < args.length && args[i] instanceof String row) {
                rows.add(row);
                i++;
            }
        }
        java.util.Map<Character, Object> key = new java.util.HashMap<>();
        for (; i + 1 < args.length; i += 2) {
            if (!(args[i] instanceof Character c)) continue;
            Object v = args[i + 1];
            if (v instanceof Block b) v = new ItemStack(b, 1, 32767);
            else if (v instanceof Item it) v = new ItemStack(it, 1, 32767);
            key.put(c, v);
        }
        for (String row : rows) width = Math.max(width, row.length());
        height = rows.size();
        input = new Object[width * height];
        for (int y = 0; y < height; y++) {
            String row = rows.get(y);
            for (int x = 0; x < row.length(); x++) input[y * width + x] = key.get(row.charAt(x));
        }
    }

    public ShapedOreRecipe setMirrored(boolean mirror) {
        mirrored = mirror;
        return this;
    }

    public Object[] getInput() {
        return input;
    }

    public static Object[] getInput(ShapedOreRecipe recipe) {
        return recipe.input;
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
