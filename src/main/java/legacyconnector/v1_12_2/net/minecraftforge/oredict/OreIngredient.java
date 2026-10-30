package legacyconnector.v1_12_2.net.minecraftforge.oredict;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;

/** An ingredient of an ore dictionary name. */
public class OreIngredient extends Ingredient {
    private final String ore;

    public OreIngredient(String ore) {
        super(OreDictionary.getOres(ore).toArray(new ItemStack[0]));
        this.ore = ore;
        this.mcc$source = ore;
    }

    public String mcc$ore() {
        return ore;
    }

    /**
     * The ore's stacks packed as the recipe book packs them (item id << 16 | metadata, 0 for damageable items and wildcards), sorted;
     * what the ore dictionary holds now, so ores registered later count.
     */
    public it.unimi.dsi.fastutil.ints.IntList getValidItemStacksPacked() {
        it.unimi.dsi.fastutil.ints.IntSortedSet packed = new it.unimi.dsi.fastutil.ints.IntRBTreeSet();
        for (ItemStack s : OreDictionary.getOres(ore)) {
            if (s.isEmpty()) continue;
            var item = s.getItem();
            int meta = s.getMetadata() == OreDictionary.WILDCARD_VALUE || item.isDamageable() ? 0 : s.getMetadata() & 0xFFFF;
            packed.add(legacyconnector.v1_12_2.net.minecraft.item.Item.getIdFromItem(item) << 16 | meta);
        }
        return new it.unimi.dsi.fastutil.ints.IntArrayList(packed);
    }

    @Override
    public boolean apply(ItemStack input) {
        if (input == null) return false;
        for (ItemStack s : OreDictionary.getOres(ore)) if (OreDictionary.itemMatches(s, input, false)) return true;
        return false;
    }
}
