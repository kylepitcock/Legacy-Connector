package legacyconnector.v1_12_2.net.minecraftforge.common.brewing;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** A brewing stand recipe: an input in a bottom slot and an ingredient in the top slot give an output. */
public interface IBrewingRecipe {
    boolean isInput(ItemStack input);

    boolean isIngredient(ItemStack ingredient);

    ItemStack getOutput(ItemStack input, ItemStack ingredient);
}
