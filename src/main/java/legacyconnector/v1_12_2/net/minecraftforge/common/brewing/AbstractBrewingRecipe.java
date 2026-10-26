package legacyconnector.v1_12_2.net.minecraftforge.common.brewing;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.OreDictionary;

public abstract class AbstractBrewingRecipe<T> implements IBrewingRecipe {
    private final ItemStack input;
    private final T ingredient;
    private final ItemStack output;

    protected AbstractBrewingRecipe(ItemStack input, T ingredient, ItemStack output) {
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
        if (getInput() == null || getIngredient() == null || getOutput() == null) {
            throw new IllegalArgumentException("A brewing recipe cannot have a null parameter.");
        }
    }

    @Override
    public boolean isInput(ItemStack stack) {
        return OreDictionary.itemMatches(getInput(), stack, false);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        return isInput(input) && isIngredient(ingredient) ? getOutput().copy() : ItemStack.EMPTY;
    }

    public ItemStack getInput() {
        return input;
    }

    public T getIngredient() {
        return ingredient;
    }

    public ItemStack getOutput() {
        return output;
    }
}
