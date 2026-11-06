package legacyconnector.v1_6_4.net.minecraft.item.crafting;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Facade of the 1.7.10 shim's {@code FurnaceRecipes} for 1.6.4 mods: smelting recipes keyed by item id. See {@code Hierarchy.facadeOf}. */
public final class FurnaceRecipes {
    private FurnaceRecipes() {}

    public static void addSmelting(legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes self, int id, ItemStack output, float xp) {
        Item item = Ids.item(id);
        if (item != null) self.addSmeltingRecipe(new ItemStack(item, 1, 32767), output, xp);
    }

    public static void addSmelting(legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes self, int id, int meta, ItemStack output, float xp) {
        Item item = Ids.item(id);
        if (item != null) self.addSmeltingRecipe(new ItemStack(item, 1, meta), output, xp);
    }
}
