package legacyconnector.v1_12_2.net.minecraft.potion;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;

/**
 * Vanilla's potion mixes (awkward potion + sugar = swiftness). Mixes mods add here become mixes of the modern brewing stand when the server
 * builds its brewing table (see {@code Brewing1122}); the vanilla mixes are the game's own.
 */
public class PotionHelper {
    public static final List<MixPredicate<PotionType>> POTION_TYPE_CONVERSIONS = new ArrayList<>();

    static {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.Brewing1122.ensureListener();
    }

    /** One mix: {@code input} brewed with {@code reagent} gives {@code output}. */
    public static class MixPredicate<T> {
        public final T input;
        public final Ingredient reagent;
        public final T output;

        public MixPredicate(T input, Ingredient reagent, T output) {
            this.input = input;
            this.reagent = reagent;
            this.output = output;
        }
    }

    public static void addMix(PotionType input, Item reagent, PotionType output) {
        addMix(input, Ingredient.fromItem(reagent), output);
    }

    public static synchronized void addMix(PotionType input, Ingredient reagent, PotionType output) {
        POTION_TYPE_CONVERSIONS.add(new MixPredicate<>(input, reagent, output));
    }

    public static boolean isReagent(ItemStack stack) {
        synchronized (PotionHelper.class) {
            for (MixPredicate<PotionType> mix : POTION_TYPE_CONVERSIONS) if (mix.reagent.apply(stack)) return true;
        }
        return new legacyconnector.v1_12_2.net.minecraftforge.common.brewing.VanillaBrewingRecipe().isIngredient(stack);
    }

    public static boolean hasConversions(ItemStack input, ItemStack reagent) {
        return hasTypeConversions(input, reagent) || !new legacyconnector.v1_12_2.net.minecraftforge.common.brewing.VanillaBrewingRecipe()
                .getOutput(input, reagent).isEmpty();
    }

    protected static synchronized boolean hasTypeConversions(ItemStack input, ItemStack reagent) {
        PotionType type = PotionUtils.getPotionFromItem(input);
        for (MixPredicate<PotionType> mix : POTION_TYPE_CONVERSIONS) if (mix.input == type && mix.reagent.apply(reagent)) return true;
        return false;
    }

    /** The brewed stack, or {@code potionIn} unchanged when nothing brews. */
    public static ItemStack doReaction(ItemStack reagent, ItemStack potionIn) {
        if (potionIn.isEmpty()) return potionIn;
        PotionType type = PotionUtils.getPotionFromItem(potionIn);
        synchronized (PotionHelper.class) {
            for (MixPredicate<PotionType> mix : POTION_TYPE_CONVERSIONS) {
                if (mix.input == type && mix.reagent.apply(reagent)) return PotionUtils.addPotionToItemStack(potionIn.copy(), mix.output);
            }
        }
        ItemStack vanilla = new legacyconnector.v1_12_2.net.minecraftforge.common.brewing.VanillaBrewingRecipe().getOutput(potionIn, reagent);
        return vanilla.isEmpty() ? potionIn : vanilla;
    }

    public static synchronized List<MixPredicate<PotionType>> mcc$mixes() {
        return new ArrayList<>(POTION_TYPE_CONVERSIONS);
    }

    public static void init() {
        // the vanilla mixes are the running game's
    }
}
