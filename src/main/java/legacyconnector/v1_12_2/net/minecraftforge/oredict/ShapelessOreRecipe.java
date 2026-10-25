package legacyconnector.v1_12_2.net.minecraftforge.oredict;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.ShapelessRecipes;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.common.crafting.CraftingHelper;

public class ShapelessOreRecipe extends ShapelessRecipes {
    protected ItemStack output = ItemStack.EMPTY;

    public ShapelessOreRecipe(ResourceLocation group, Block result, Object... recipe) {
        this(group, new ItemStack(result), recipe);
    }

    public ShapelessOreRecipe(ResourceLocation group, Item result, Object... recipe) {
        this(group, new ItemStack(result), recipe);
    }

    public ShapelessOreRecipe(ResourceLocation group, ItemStack result, Object... recipe) {
        super(group == null ? "" : group.toString(), result, ingredients(recipe));
        this.output = result;
    }

    public ShapelessOreRecipe(ResourceLocation group, NonNullList<Ingredient> input, ItemStack result) {
        super(group == null ? "" : group.toString(), result, input);
        this.output = result;
    }

    private static NonNullList<Ingredient> ingredients(Object[] recipe) {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Object o : recipe) list.add(CraftingHelper.getIngredient(o));
        return list;
    }
}
