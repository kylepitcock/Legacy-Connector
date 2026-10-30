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
    /** Whether every ingredient is a simple item match (vanilla's faster matcher could be used). */
    protected boolean isSimple = true;
    protected ResourceLocation group;
    /** Phantom field: {@link #mcc$get$input} (the ingredients, kept by the vanilla recipe as {@code recipeItems}). */
    @Deprecated
    protected NonNullList<Ingredient> input;

    public static NonNullList<Ingredient> mcc$get$input(ShapelessOreRecipe r) {
        return r.recipeItems;
    }

    public static void mcc$set$input(ShapelessOreRecipe r, NonNullList<Ingredient> v) {
        if (v == r.recipeItems) return;
        r.recipeItems.clear();
        if (v != null) r.recipeItems.addAll(v);
    }

    public ShapelessOreRecipe(ResourceLocation group, Block result, Object... recipe) {
        this(group, new ItemStack(result), recipe);
    }

    public ShapelessOreRecipe(ResourceLocation group, Item result, Object... recipe) {
        this(group, new ItemStack(result), recipe);
    }

    public ShapelessOreRecipe(ResourceLocation group, ItemStack result, Object... recipe) {
        super(group == null ? "" : group.toString(), result, ingredients(recipe));
        this.output = result;
        this.group = group;
        for (Ingredient i : recipeItems) isSimple &= i.isSimple();
    }

    public ShapelessOreRecipe(ResourceLocation group, NonNullList<Ingredient> input, ItemStack result) {
        super(group == null ? "" : group.toString(), result, input);
        this.output = result;
        this.group = group;
        for (Ingredient i : recipeItems) isSimple &= i.isSimple();
    }

    private static NonNullList<Ingredient> ingredients(Object[] recipe) {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Object o : recipe) list.add(CraftingHelper.getIngredient(o));
        return list;
    }

    /** Forge: a shapeless ore recipe from its JSON (mods' recipe factories build on it). */
    public static ShapelessOreRecipe factory(legacyconnector.v1_12_2.net.minecraftforge.common.crafting.JsonContext context, com.google.gson.JsonObject json) {
        com.google.gson.JsonObject copy = json.deepCopy();
        copy.addProperty("type", "forge:ore_shapeless");
        return (ShapelessOreRecipe) legacyconnector.v1_12_2.net.minecraftforge.common.crafting.CraftingHelper.getRecipe(copy, context);
    }
}
