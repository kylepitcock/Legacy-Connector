package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect;
import legacyconnector.v1_7_10.net.minecraft.potion.PotionHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10 brewing in the modern brewing stand. 1.7.10 potions are a 15-bit value that ingredients change through their
 * {@code Item.getPotionEffect} expression ({@link PotionHelper#applyIngredient}); the effects follow from the value and
 * {@code PotionHelper.potionRequirements}, where mods add their own potions. One brewing recipe runs that: a modern potion
 * becomes its 1.7.10 value, a mod ingredient (or any ingredient on a potion made this way) changes it as in 1.7.10, and the
 * result is the vanilla potion with those effects or, for mod effects, a potion that keeps its 1.7.10 value. Vanilla
 * ingredients on vanilla potions stay with the modern mixes.
 */
public final class Brewing1710 {
    private static final String TAG = "legacyconnector:potion";
    private static final int SPLASH = 16384;
    /** Brewed 1.7.10 values of the vanilla potions (without the splash bit). */
    private static final Map<Integer, Holder<Potion>> VANILLA = new HashMap<>();
    private static final Map<Holder<Potion>, Integer> VALUE = new HashMap<>();
    private static boolean listening;

    private Brewing1710() {}

    private static void put(int value, Holder<Potion> potion) {
        VANILLA.put(value, potion);
        VALUE.putIfAbsent(potion, value);
    }

    static {
        put(0, Potions.WATER);
        put(16, Potions.AWKWARD);
        put(32, Potions.THICK);
        put(8192, Potions.MUNDANE);
        put(64, Potions.MUNDANE);
        put(8193, Potions.REGENERATION);
        put(8257, Potions.LONG_REGENERATION);
        put(8225, Potions.STRONG_REGENERATION);
        put(8194, Potions.SWIFTNESS);
        put(8258, Potions.LONG_SWIFTNESS);
        put(8226, Potions.STRONG_SWIFTNESS);
        put(8195, Potions.FIRE_RESISTANCE);
        put(8259, Potions.LONG_FIRE_RESISTANCE);
        put(8196, Potions.POISON);
        put(8260, Potions.LONG_POISON);
        put(8228, Potions.STRONG_POISON);
        put(8197, Potions.HEALING);
        put(8229, Potions.STRONG_HEALING);
        put(8198, Potions.NIGHT_VISION);
        put(8262, Potions.LONG_NIGHT_VISION);
        put(8200, Potions.WEAKNESS);
        put(8264, Potions.LONG_WEAKNESS);
        put(8201, Potions.STRENGTH);
        put(8265, Potions.LONG_STRENGTH);
        put(8233, Potions.STRONG_STRENGTH);
        put(8202, Potions.SLOWNESS);
        put(8266, Potions.LONG_SLOWNESS);
        put(8204, Potions.HARMING);
        put(8236, Potions.STRONG_HARMING);
        put(8205, Potions.WATER_BREATHING);
        put(8269, Potions.LONG_WATER_BREATHING);
        put(8206, Potions.INVISIBILITY);
        put(8270, Potions.LONG_INVISIBILITY);
    }

    /** Installed once 1.7.10 mods are loaded (the event fires on every server start and data reload). */
    public static synchronized void ensureListener() {
        if (listening) return;
        listening = true;
        GeneratedTranslations.put("en_us", "legacyconnector.potion_of", "Potion of %s");
        GeneratedTranslations.put("en_us", "legacyconnector.splash_potion_of", "Splash Potion of %s");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(RegisterBrewingRecipesEvent.class, e -> e.getBuilder().addRecipe(new LegacyBrewing()));
    }

    /** The 1.7.10 value of a modern potion stack, or -1. */
    static int valueOf(ItemStack stack) {
        boolean splash = stack.is(Items.SPLASH_POTION);
        if (!splash && !stack.is(Items.POTION)) return -1;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(TAG)) return data.copyTag().getInt(TAG);
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty() || !contents.customEffects().isEmpty()) return -1;
        Integer value = VALUE.get(contents.potion().get());
        return value == null ? -1 : value | (splash ? SPLASH : 0);
    }

    /** Effects of a 1.7.10 value as comparable text (id, amplifier, duration), or null for none. */
    private static String signature(int value) {
        List<?> effects = PotionHelper.getPotionEffects(value, (value & SPLASH) != 0);
        if (effects == null || effects.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        for (Object o : effects) {
            PotionEffect e = (PotionEffect) o;
            parts.add(e.getPotionID() + ":" + e.getAmplifier() + ":" + e.getDuration());
        }
        parts.sort(null);
        return String.join(",", parts);
    }

    /** The modern stack for a 1.7.10 value. */
    static ItemStack stackOf(int value) {
        boolean splash = (value & SPLASH) != 0;
        int plain = value & ~SPLASH;
        ItemStack stack = new ItemStack(splash ? Items.SPLASH_POTION : Items.POTION);
        Holder<Potion> vanilla = VANILLA.get(plain);
        String signature = signature(plain);
        if (vanilla == null) {
            for (var e : VANILLA.entrySet()) {
                if (java.util.Objects.equals(signature, signature(e.getKey())) && (signature != null || (e.getKey() & 63) == (plain & 63))) {
                    vanilla = e.getValue();
                    break;
                }
            }
        }
        if (vanilla == null && signature == null) {
            int base = plain & 63;
            vanilla = plain == 0 ? Potions.WATER : base == 16 ? Potions.AWKWARD : base == 32 ? Potions.THICK : Potions.MUNDANE;
        }
        if (vanilla != null) {
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(vanilla));
            return stack;
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        Component name = null;
        for (Object o : PotionHelper.getPotionEffects(value, splash)) {
            PotionEffect e = (PotionEffect) o;
            Holder<MobEffect> effect = PotionBridge.effectOrNull(e.getPotionID());
            if (effect == null) continue;
            effects.add(new MobEffectInstance(effect, e.getDuration(), e.getAmplifier()));
            if (name == null) name = effect.value().getDisplayName();
        }
        if (effects.isEmpty()) {
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.MUNDANE));
            return stack;
        }
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(0xFF000000 | PotionHelper.func_77915_a(value, false)), effects));
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ITEM_NAME, Component.translatable(splash ? "legacyconnector.splash_potion_of" : "legacyconnector.potion_of", name));
        return stack;
    }

    private static final class LegacyBrewing implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            return valueOf(input) >= 0;
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            if (ingredient.isEmpty() || BuiltInRegistries.ITEM.getKey(ingredient.getItem()).getNamespace().equals("minecraft")) return false;
            return expression(ingredient) != null;
        }

        /** The ingredient's 1.7.10 effect expression, or null. */
        private static String expression(ItemStack ingredient) {
            // the pufferfish was a subtype of 1.7.10's fish item, which answered by metadata
            if (ingredient.is(Items.PUFFERFISH)) return PotionHelper.field_151423_m;
            try {
                var view = legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(ingredient);
                Item item = view == null ? null : view.getItem();
                return item != null && item.isPotionIngredient(view) ? item.getPotionEffect(view) : null;
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (ingredient.isEmpty()) return ItemStack.EMPTY;
            boolean modIngredient = !BuiltInRegistries.ITEM.getKey(ingredient.getItem()).getNamespace().equals("minecraft");
            CustomData data = input.get(DataComponents.CUSTOM_DATA);
            boolean legacyPotion = data != null && data.contains(TAG);
            if (!modIngredient && !legacyPotion) return ItemStack.EMPTY; // vanilla on vanilla: the modern mixes
            int from = valueOf(input);
            String expression = expression(ingredient);
            if (from < 0 || expression == null) return ItemStack.EMPTY;
            try {
                int to = PotionHelper.applyIngredient(from, expression);
                // as the 1.7.10 brewing stand: something must change, and an effect must not be brewed away
                String before = signature(from), after = signature(to);
                if (to == from || before != null && (after == null || before.equals(after))) return ItemStack.EMPTY;
                return stackOf(to);
            } catch (RuntimeException | LinkageError e) {
                LoggerFactory.getLogger("LegacyConnector/Brewing").warn("Legacy brewing failed for {} on {}: {}", ingredient, input, e.toString());
                return ItemStack.EMPTY;
            }
        }
    }
}
