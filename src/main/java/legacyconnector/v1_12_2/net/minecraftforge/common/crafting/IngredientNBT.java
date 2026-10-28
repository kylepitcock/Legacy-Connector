package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;

/** Forge: an ingredient matching one exact stack, its NBT included. */
public class IngredientNBT extends Ingredient {
    private final ItemStack stack;

    public IngredientNBT(ItemStack stack) {
        super(stack);
        this.stack = stack;
    }

    @Override
    public boolean apply(ItemStack input) {
        if (input == null) return false;
        return stack.getItem() == input.getItem() && stack.getItemDamage() == input.getItemDamage()
                && ItemStack.areItemStackShareTagsEqual(stack, input);
    }

    @Override
    public boolean isSimple() {
        return false;
    }
}
