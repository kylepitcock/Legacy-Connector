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
    }
}
