package legacyconnector.v1_12_2.net.minecraftforge.common.brewing;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.OreDictionary;

/** A brewing recipe whose ingredient is any stack of an ore dictionary name (or of a list). */
public class BrewingOreRecipe extends AbstractBrewingRecipe<List<ItemStack>> {
    public BrewingOreRecipe(ItemStack input, String ingredient, ItemStack output) {
        super(input, OreDictionary.getOres(ingredient), output);
    }

    public BrewingOreRecipe(ItemStack input, List<ItemStack> ingredient, ItemStack output) {
        super(input, ingredient, output);
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        for (ItemStack target : getIngredient()) if (OreDictionary.itemMatches(target, stack, false)) return true;
        return false;
    }
}
