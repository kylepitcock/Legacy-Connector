package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The static animal food fields of 1.20.1 ({@code Chicken.FOOD_ITEMS}, {@code Pig.FOOD_ITEMS}, {@code Cat/Ocelot.TEMPT_INGREDIENT},
 * {@code Strider.FOOD_ITEMS/TEMPT_ITEMS}, {@code AbstractHorse.FOOD_ITEMS}, {@code Parrot.TAME_FOOD}) that 1.21.1 replaced by item tags
 * ({@code ItemTags.CHICKEN_FOOD}...). A READ answers an {@link Ingredient} of the tag (so what packs add to the tag shows through). A WRITE
 * ({@code Chicken.FOOD_ITEMS = Ingredient.merge(...)}, the usual way a mod adds its crops) is remembered as the animal's food set, and
 * {@code AnimalFoodHookMixin} lets {@code isFood} answer from it.
 *
 * <p>APPROXIMATIONS: a written set is held in memory (not written into tag data), it decides {@code isFood} only (feeding, breeding, taming of cats and
 * ocelots), not the tempt goals, which read their own tags in 1.21.1; {@code Parrot.TAME_FOOD} is a live view (the parrot tag plus what a mod adds) that
 * the parrot's taming does not consult.
 */
public final class AnimalFood1201 {
    private AnimalFood1201() {}

    /** Kind to the food set a mod wrote (absent: nobody wrote it, the tag decides). */
    private static final Map<String, Ingredient> WRITTEN = new ConcurrentHashMap<>();

    private static Ingredient tagIngredient(TagKey<Item> tag) {
        return Ingredient.fromValues(Stream.of(new Ingredient.TagValue(tag)));
    }

    private static Ingredient read(String kind, TagKey<Item> tag) {
        Ingredient written = WRITTEN.get(kind);
        return written != null ? written : tagIngredient(tag);
    }

    public static Ingredient chicken() {
        return read("chicken", ItemTags.CHICKEN_FOOD);
    }

    public static Ingredient pig() {
        return read("pig", ItemTags.PIG_FOOD);
    }

    public static Ingredient cat() {
        return read("cat", ItemTags.CAT_FOOD);
    }

    public static Ingredient ocelot() {
        return read("ocelot", ItemTags.OCELOT_FOOD);
    }

    public static Ingredient strider() {
        return read("strider", ItemTags.STRIDER_FOOD);
    }

    public static Ingredient striderTempt() {
        return read("strider_tempt", ItemTags.STRIDER_TEMPT_ITEMS);
    }

    public static Ingredient horse() {
        return read("horse", ItemTags.HORSE_FOOD);
    }

    public static void setChicken(Ingredient food) {
        WRITTEN.put("chicken", food);
    }

    public static void setPig(Ingredient food) {
        WRITTEN.put("pig", food);
    }

    public static void setCat(Ingredient food) {
        WRITTEN.put("cat", food);
    }

    public static void setOcelot(Ingredient food) {
        WRITTEN.put("ocelot", food);
    }

    public static void setStrider(Ingredient food) {
        WRITTEN.put("strider", food);
    }

    public static void setStriderTempt(Ingredient food) {
        WRITTEN.put("strider_tempt", food);
    }

    public static void setHorse(Ingredient food) {
        WRITTEN.put("horse", food);
    }

    /** The hook of {@code isFood}: the set a mod wrote for this kind of animal decides, otherwise vanilla's answer stands. */
    public static boolean isFood(Object animal, ItemStack stack, boolean vanilla) {
        if (WRITTEN.isEmpty()) return vanilla;
        String kind = animal instanceof Chicken ? "chicken"
                : animal instanceof Pig ? "pig"
                : animal instanceof Cat ? "cat"
                : animal instanceof Ocelot ? "ocelot"
                : animal instanceof Strider ? "strider"
                : animal instanceof AbstractHorse ? "horse" : null;
        if (kind == null) return vanilla;
        Ingredient written = WRITTEN.get(kind);
        return written != null ? written.test(stack) : vanilla;
    }

    /** {@code Parrot.TAME_FOOD = set}: the items the mod's set holds are added to the parrot food (APPROXIMATION: a set that drops vanilla items does not remove them). */
    public static void setParrotTame(Set<?> food) {
        if (food == null) return;
        for (Object o : food) if (o instanceof Item item) TAME_EXTRA.add(item);
    }

    private static final Set<Item> TAME_EXTRA = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** {@code Parrot.TAME_FOOD}: the parrot food tag plus what a mod adds; a mutable set like the 1.20.1 one. */
    public static Set<Item> parrotTame() {
        return PARROT_TAME;
    }

    private static final Set<Item> PARROT_TAME = new AbstractSet<>() {
        @Override
        public boolean contains(Object o) {
            return o instanceof Item item && (TAME_EXTRA.contains(item) || item.builtInRegistryHolder().is(ItemTags.PARROT_FOOD));
        }

        @Override
        public boolean add(Item item) {
            return !contains(item) && TAME_EXTRA.add(item);
        }

        @Override
        public boolean remove(Object o) {
            return TAME_EXTRA.remove(o);
        }

        @Override
        public Iterator<Item> iterator() {
            return Stream.concat(BuiltInRegistries.ITEM.stream().filter(i -> i.builtInRegistryHolder().is(ItemTags.PARROT_FOOD)), TAME_EXTRA.stream()).distinct().iterator();
        }

        @Override
        public int size() {
            return (int) Stream.concat(BuiltInRegistries.ITEM.stream().filter(i -> i.builtInRegistryHolder().is(ItemTags.PARROT_FOOD)), TAME_EXTRA.stream()).distinct().count();
        }
    };
}
