package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.OreIngredient;

public class CraftingHelper {
    /** A parsed shaped-recipe description. */
    public static class ShapedPrimer {
        public int width;
        public int height;
        public boolean mirrored = true;
        public NonNullList<Ingredient> input = NonNullList.create();
    }

    public static Ingredient getIngredient(Object obj) {
        if (obj instanceof Ingredient i) return i;
        if (obj instanceof ItemStack s) return s.isEmpty() ? Ingredient.EMPTY : Ingredient.fromStacks(s);
        if (obj instanceof Item item) return Ingredient.fromItem(item);
        if (obj instanceof Block block) return Ingredient.fromBlock(block);
        if (obj instanceof String ore) return new OreIngredient(ore);
        if (obj instanceof List<?> list) {
            ItemStack[] stacks = list.stream().filter(e -> e instanceof ItemStack).map(e -> (ItemStack) e).toArray(ItemStack[]::new);
            return Ingredient.fromStacks(stacks);
        }
        return Ingredient.EMPTY;
    }

    public static ShapedPrimer parseShaped(Object... recipe) {
        ShapedPrimer primer = new ShapedPrimer();
        int i = 0;
        if (recipe.length > 0 && recipe[0] instanceof Boolean b) {
            primer.mirrored = b;
            i = 1;
        }
        java.util.ArrayList<String> rows = new java.util.ArrayList<>();
        if (i < recipe.length && recipe[i] instanceof String[] arr) {
            for (String row : arr) rows.add(row);
            i++;
        } else {
            while (i < recipe.length && recipe[i] instanceof String row) {
                rows.add(row);
                i++;
            }
        }
        Map<Character, Ingredient> key = new HashMap<>();
        key.put(' ', Ingredient.EMPTY);
        for (; i + 1 < recipe.length; i += 2) {
            if (recipe[i] instanceof Character c) key.put(c, getIngredient(recipe[i + 1]));
        }
        primer.height = rows.size();
        primer.width = rows.isEmpty() ? 0 : rows.get(0).length();
        for (String row : rows) {
            for (int x = 0; x < primer.width; x++) {
                Ingredient ing = x < row.length() ? key.get(row.charAt(x)) : null;
                primer.input.add(ing == null ? Ingredient.EMPTY : ing);
            }
        }
        return primer;
    }
}
