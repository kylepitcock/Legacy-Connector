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

    @Override
    public boolean apply(ItemStack input) {
        if (input == null) return false;
        for (ItemStack s : OreDictionary.getOres(ore)) if (OreDictionary.itemMatches(s, input, false)) return true;
        return false;
    }
}
