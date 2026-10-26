package legacyconnector.v1_12_2.net.minecraftforge.common.brewing;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.OreDictionary;

public class BrewingRecipe extends AbstractBrewingRecipe<ItemStack> {
    public BrewingRecipe(ItemStack input, ItemStack ingredient, ItemStack output) {
        super(input, ingredient, output);
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        return OreDictionary.itemMatches(getIngredient(), stack, false);
    }
}
