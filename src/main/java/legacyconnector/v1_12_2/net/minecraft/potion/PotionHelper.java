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
    /** The items the brewing stand takes as potions (potion, splash and lingering bottles; mods add their own containers). */
    public static final List<Ingredient> POTION_ITEMS = new ArrayList<>(List.of(
            Ingredient.fromItem(org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.itemForModern(net.minecraft.world.item.Items.POTION)),
            Ingredient.fromItem(org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.itemForModern(net.minecraft.world.item.Items.SPLASH_POTION)),
            Ingredient.fromItem(org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.itemForModern(net.minecraft.world.item.Items.LINGERING_POTION))));

    /** The vanilla container changes (a potion with gunpowder becomes a splash potion, a splash one with dragon's breath lingering). */
    public static final List<MixPredicate<Item>> POTION_ITEM_CONVERSIONS = new ArrayList<>(List.of(
            new MixPredicate<>(org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.itemForModern(net.minecraft.world.item.Items.POTION),
                    Ingredient.fromItem(org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.itemForModern(net.minecraft.world.item.Items.GUNPOWDER)),
                    org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.itemForModern(net.minecraft.world.item.Items.SPLASH_POTION)),
            new MixPredicate<>(org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.itemForModern(net.minecraft.world.item.Items.SPLASH_POTION),
                    Ingredient.fromItem(org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.itemForModern(net.minecraft.world.item.Items.DRAGON_BREATH)),
                    org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.itemForModern(net.minecraft.world.item.Items.LINGERING_POTION))));
    /** Whether a stack is one of the brewing stand's potion containers ({@link #POTION_ITEMS}). */
    public static final com.google.common.base.Predicate<ItemStack> IS_POTION_ITEM = stack -> {
        for (Ingredient i : POTION_ITEMS) if (i.apply(stack)) return true;
        return false;
    };

    static {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.Brewing1122.ensureListener();
    }

    /**
     * One mix: {@code input} brewed with {@code reagent} gives {@code output}. Forge keeps the potion types (or items) as registry
     * delegates, which mods read through {@code input.get()}.
     */
    public static class MixPredicate<T> {
        public final legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<T> input;
        public final Ingredient reagent;
        public final legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<T> output;

        public MixPredicate(T input, Ingredient reagent, T output) {
            this.input = mcc$delegate(input);
            this.reagent = reagent;
            this.output = mcc$delegate(output);
        }

        public T mcc$input() {
            return input == null ? null : input.get();
        }

        public T mcc$output() {
            return output == null ? null : output.get();
        }

        @SuppressWarnings("unchecked")
        private static <T> legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<T> mcc$delegate(T value) {
            if (value instanceof legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry.Impl<?> impl && impl.delegate != null) {
                return (legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<T>) impl.delegate;
            }
            return new legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<>() {
                @Override
                public T get() {
                    return value;
                }

                @Override
                public legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation name() {
                    return value instanceof legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry<?> e ? e.getRegistryName() : null;
                }

                @Override
                @SuppressWarnings("unchecked")
                public Class<T> type() {
                    return value == null ? null : (Class<T>) value.getClass();
                }
            };
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
        for (MixPredicate<PotionType> mix : POTION_TYPE_CONVERSIONS) if (mix.mcc$input() == type && mix.reagent.apply(reagent)) return true;
        return false;
    }

    /** The brewed stack, or {@code potionIn} unchanged when nothing brews. */
    public static ItemStack doReaction(ItemStack reagent, ItemStack potionIn) {
        if (potionIn.isEmpty()) return potionIn;
        PotionType type = PotionUtils.getPotionFromItem(potionIn);
        synchronized (PotionHelper.class) {
            for (MixPredicate<PotionType> mix : POTION_TYPE_CONVERSIONS) {
                if (mix.mcc$input() == type && mix.reagent.apply(reagent)) return PotionUtils.addPotionToItemStack(potionIn.copy(), mix.mcc$output());
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
