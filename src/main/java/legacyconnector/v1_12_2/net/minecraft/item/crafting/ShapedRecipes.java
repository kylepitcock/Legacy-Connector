package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

public class ShapedRecipes extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe, legacyconnector.v1_12_2.net.minecraftforge.common.crafting.IShapedRecipe {
    public final int recipeWidth;
    public final int recipeHeight;
    public final NonNullList<Ingredient> recipeItems;
    protected final ItemStack recipeOutput;
    protected final String group;

    public ShapedRecipes(String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
        this.group = group;
        this.recipeWidth = width;
        this.recipeHeight = height;
        this.recipeItems = ingredients;
        this.recipeOutput = result;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return recipeOutput;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width >= recipeWidth && height >= recipeHeight;
    }

    /** Whether the grid holds the pattern anywhere in it (also mirrored), everything else empty. */
    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        for (int i = 0; i <= inv.getWidth() - recipeWidth; ++i) {
            for (int j = 0; j <= inv.getHeight() - recipeHeight; ++j) {
                if (checkMatch(inv, i, j, true)) return true;
                if (checkMatch(inv, i, j, false)) return true;
            }
        }
        return false;
    }

    /** Whether the pattern placed at (x, y) of the grid (mirrored or not) matches. */
    protected boolean checkMatch(InventoryCrafting craftingInventory, int startX, int startY, boolean mirror) {
        int width = mcc$width();
        int height = mcc$height();
        NonNullList<Ingredient> items = mcc$items();
        for (int i = 0; i < craftingInventory.getWidth(); ++i) {
            for (int j = 0; j < craftingInventory.getHeight(); ++j) {
                int k = i - startX;
                int l = j - startY;
                Ingredient ingredient = Ingredient.EMPTY;
                if (k >= 0 && l >= 0 && k < width && l < height) {
                    ingredient = mirror ? items.get(width - k - 1 + l * width) : items.get(k + l * width);
                }
                if (!ingredient.apply(craftingInventory.getStackInRowAndColumn(i, j))) return false;
            }
        }
        return true;
    }

    /** The pattern used by matching (subclasses with their own fields hand theirs). */
    protected int mcc$width() {
        return recipeWidth;
    }

    protected int mcc$height() {
        return recipeHeight;
    }

    protected NonNullList<Ingredient> mcc$items() {
        return recipeItems;
    }

    @Override
    public int getRecipeWidth() {
        return recipeWidth;
    }

    @Override
    public int getRecipeHeight() {
        return recipeHeight;
    }

    /** The pattern rows turned into a list of ingredients (row by row, keys looked up, spaces empty). */
    public static NonNullList<Ingredient> deserializeIngredients(String[] pattern, java.util.Map<String, Ingredient> keys, int patternWidth, int patternHeight) {
        NonNullList<Ingredient> list = NonNullList.withSize(patternWidth * patternHeight, Ingredient.EMPTY);
        java.util.Set<String> unused = new java.util.HashSet<>(keys.keySet());
        unused.remove(" ");
        for (int i = 0; i < pattern.length; ++i) {
            for (int j = 0; j < pattern[i].length(); ++j) {
                String s = pattern[i].substring(j, j + 1);
                Ingredient ingredient = keys.get(s);
                if (ingredient == null) throw new com.google.gson.JsonSyntaxException("Pattern references symbol '" + s + "' but it's not defined in the key");
                unused.remove(s);
                list.set(j + patternWidth * i, ingredient);
            }
        }
        if (!unused.isEmpty()) throw new com.google.gson.JsonSyntaxException("Key defines symbols that aren't used in pattern: " + unused);
        return list;
    }

    /** An item stack from recipe JSON ({@code item}, {@code data}, and {@code count} when useCount). */
    public static ItemStack deserializeItem(com.google.gson.JsonObject json, boolean useCount) {
        String name = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(json, "item");
        legacyconnector.v1_12_2.net.minecraft.item.Item item = legacyconnector.v1_12_2.net.minecraft.item.Item.REGISTRY.getObject(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(name));
        if (item == null) throw new com.google.gson.JsonSyntaxException("Unknown item '" + name + "'");
        if (item.getHasSubtypes() && !json.has("data")) throw new com.google.gson.JsonParseException("Missing data for item '" + name + "'");
        int data = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getInt(json, "data", 0);
        int count = useCount ? legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getInt(json, "count", 1) : 1;
        return new ItemStack(item, count, data);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return getRecipeOutput().copy();
    }

    public int getWidth() {
        return recipeWidth;
    }

    public int getHeight() {
        return recipeHeight;
    }
}
