package legacyconnector.v1_20_1.net.minecraftforge.common.brewing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyBrewing;

/**
 * Forge 1.20.1's {@code VanillaBrewingRecipe}: the recipe that stands for the vanilla potion mixes (NeoForge 1.21.1 dropped the class: vanilla's mixes live in
 * {@link PotionBrewing} itself). Answers from the brewing registry in use; before a server or level exists it answers "no recipe".
 */
public class VanillaBrewingRecipe implements IBrewingRecipe {
    @Override
    public boolean isInput(ItemStack input) {
        PotionBrewing b = LegacyBrewing.current();
        return b != null && b.isInput(input);
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        PotionBrewing b = LegacyBrewing.current();
        return b != null && b.isIngredient(ingredient);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        PotionBrewing b = LegacyBrewing.current();
        if (b == null || input.isEmpty() || ingredient.isEmpty() || !b.hasMix(ingredient, input)) return ItemStack.EMPTY;
        ItemStack out = b.mix(ingredient, input);
        return out == null ? ItemStack.EMPTY : out;
    }
}
