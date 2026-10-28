package legacyconnector.v1_12_2.net.minecraftforge.oredict;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.ShapedRecipes;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.common.crafting.CraftingHelper;

/** A shaped recipe whose ingredients may be ore dictionary names. */
public class ShapedOreRecipe extends ShapedRecipes {
    /** The 1.12 field names mods read. */
    protected ItemStack output = ItemStack.EMPTY;
    protected NonNullList<Ingredient> input = NonNullList.create();
    protected int width;
    protected int height;
    protected boolean mirrored = true;
    protected ResourceLocation group;

    public ShapedOreRecipe(ResourceLocation group, Block result, Object... recipe) {
        this(group, new ItemStack(result), recipe);
    }

    public ShapedOreRecipe(ResourceLocation group, Item result, Object... recipe) {
        this(group, new ItemStack(result), recipe);
    }

    public ShapedOreRecipe(ResourceLocation group, ItemStack result, Object... recipe) {
        this(group, result, CraftingHelper.parseShaped(recipe));
    }

    public ShapedOreRecipe(ResourceLocation group, ItemStack result, CraftingHelper.ShapedPrimer primer) {
        super(group == null ? "" : group.toString(), primer.width, primer.height, primer.input, result);
        this.output = result;
        this.width = primer.width;
        this.height = primer.height;
        this.input = primer.input;
        this.mirrored = primer.mirrored;
        this.group = group;
    }

    @Override
    protected int mcc$width() {
        return width;
    }

    @Override
    protected int mcc$height() {
        return height;
    }

    @Override
    protected NonNullList<Ingredient> mcc$items() {
        return input;
    }

    @Override
    public boolean matches(legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting inv, legacyconnector.v1_12_2.net.minecraft.world.World world) {
        for (int x = 0; x <= inv.getWidth() - width; x++) {
            for (int y = 0; y <= inv.getHeight() - height; ++y) {
                if (checkMatch(inv, x, y, false)) return true;
                if (mirrored && checkMatch(inv, x, y, true)) return true;
            }
        }
        return false;
    }

    @Override
    protected boolean checkMatch(legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting inv, int startX, int startY, boolean mirror) {
        return super.checkMatch(inv, startX, startY, mirror);
    }

    public ShapedOreRecipe setMirrored(boolean mirror) {
        this.mirrored = mirror;
        return this;
    }

    /** Forge IShapedRecipe: the pattern's width and height. */
    @Override
    public int getRecipeWidth() {
        return width;
    }

    @Override
    public int getRecipeHeight() {
        return height;
    }

    /** Forge: a shaped ore recipe from its JSON (mods' recipe factories build on it). */
    public static ShapedOreRecipe factory(legacyconnector.v1_12_2.net.minecraftforge.common.crafting.JsonContext context, com.google.gson.JsonObject json) {
        com.google.gson.JsonObject copy = json.deepCopy();
        copy.addProperty("type", "forge:ore_shaped");
        return (ShapedOreRecipe) legacyconnector.v1_12_2.net.minecraftforge.common.crafting.CraftingHelper.getRecipe(copy, context);
    }
}
