package legacyconnector.v1_6_4.facade.net.minecraft.item.crafting;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Facade of the 1.7.10 shim's {@code FurnaceRecipes} for 1.6.4 mods: smelting recipes keyed by item id. See {@code Hierarchy.facadeOf}. */
public final class FurnaceRecipes {
    private FurnaceRecipes() {}

    /**
     * 1.6.4 {@code getSmeltingList()}: the recipes for any damage as a map keyed by item id (the 1.7.10 shim's is keyed by input stack). A live
     * view: a put registers the recipe, a get finds it. (Reached through the {@code mcc$legacy$} prefix: the shim has a method of that name.)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static java.util.Map mcc$legacy$getSmeltingList(legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes self) {
        return new java.util.AbstractMap<Object, Object>() {
            @Override
            public Object put(Object key, Object value) {
                if (key instanceof Integer id && value instanceof ItemStack out) {
                    Item item = Ids.item(id);
                    if (item != null) self.addSmeltingRecipe(new ItemStack(item, 1, 32767), out, 0.0F);
                }
                return null;
            }

            @Override
            public Object get(Object key) {
                if (!(key instanceof Integer id)) return null;
                Item item = Ids.item(id);
                return item == null ? null : self.getSmeltingResult(new ItemStack(item, 1, 32767));
            }

            @Override
            public boolean containsKey(Object key) {
                return get(key) != null;
            }

            @Override
            public java.util.Set<Entry<Object, Object>> entrySet() {
                java.util.Set<Entry<Object, Object>> out = new java.util.LinkedHashSet<>();
                for (Object o : self.getSmeltingList().entrySet()) {
                    Entry<?, ?> e = (Entry<?, ?>) o;
                    if (e.getKey() instanceof ItemStack in && in.getItem() != null && in.getItemDamage() == 32767) {
                        out.add(new java.util.AbstractMap.SimpleEntry<>(Ids.itemId(in.getItem()), e.getValue()));
                    }
                }
                return out;
            }
        };
    }

    public static void addSmelting(legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes self, int id, ItemStack output, float xp) {
        Item item = Ids.item(id);
        if (item != null) self.addSmeltingRecipe(new ItemStack(item, 1, 32767), output, xp);
    }

    public static void addSmelting(legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes self, int id, int meta, ItemStack output, float xp) {
        Item item = Ids.item(id);
        if (item != null) self.addSmeltingRecipe(new ItemStack(item, 1, meta), output, xp);
    }
}
