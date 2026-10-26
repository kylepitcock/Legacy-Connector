package legacyconnector.v1_12_2.net.minecraftforge.common.brewing;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/** Vanilla's brewing (potion mixes and container changes): the running game's own brewing table. */
public class VanillaBrewingRecipe implements IBrewingRecipe {
    private static net.minecraft.world.item.alchemy.PotionBrewing brewing() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.potionBrewing();
    }

    @Override
    public boolean isInput(ItemStack stack) {
        var b = brewing();
        return b != null && b.isInput(Stacks1122.toModern(stack));
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        var b = brewing();
        return b != null && b.isIngredient(Stacks1122.toModern(stack));
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        var b = brewing();
        if (b == null || input.isEmpty() || ingredient.isEmpty() || !isIngredient(ingredient)) return ItemStack.EMPTY;
        var modernInput = Stacks1122.toModern(input);
        var modernIngredient = Stacks1122.toModern(ingredient);
        if (!b.hasMix(modernInput, modernIngredient)) return ItemStack.EMPTY;
        return Stacks1122.fromModern(b.mix(modernIngredient, modernInput));
    }
}
