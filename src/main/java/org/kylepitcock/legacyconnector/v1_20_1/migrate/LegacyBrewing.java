package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import legacyconnector.v1_20_1.net.minecraftforge.registries.IForgeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * The 1.20.1 brewing API on top of 1.21.1's {@code PotionBrewing.Builder}. In 1.20.1 mods added to static lists of {@code PotionBrewing}
 * ({@code POTION_MIXES}, {@code CONTAINER_MIXES}, {@code ALLOWED_CONTAINERS}), called {@code PotionBrewing.addMix} and
 * {@code BrewingRecipeRegistry.addRecipe}; 1.21.1 builds an immutable {@code PotionBrewing} per server/client from
 * {@code RegisterBrewingRecipesEvent}. Everything a mod adds is therefore kept here and replayed into the builder of every event, so it ends up in
 * the real brewing recipes (brewing stand, JEI).
 *
 * <p>Reading the static lists answers the vanilla mixes of the {@code PotionBrewing} in use (server, else the client's level) followed by the
 * mixes mods added; a read before any world exists sees the added ones only. {@link Mix} stands in for the package-private
 * {@code PotionBrewing.Mix} record the old code constructed and read the fields of.
 *
 * <p>Approximations: a mod's potion mix works on any item carrying a potion of the "from" type (1.20.1 required one of its containers), the
 * container list view reports vanilla containers and added ones separately only through iteration order.
 */
public final class LegacyBrewing {
    private LegacyBrewing() {}

    /** Stand-in for {@code PotionBrewing.Mix}: a mix from one potion (or container item) to another through an ingredient. */
    public static final class Mix<T> {
        public final Holder.Reference<T> from;
        public final Ingredient ingredient;
        public final Holder.Reference<T> to;

        public Mix(Holder.Reference<T> from, Ingredient ingredient, Holder.Reference<T> to) {
            this.from = from;
            this.ingredient = ingredient;
            this.to = to;
        }

        /** 1.20.1 Forge's constructor: the registry resolves the values to their holders. */
        public Mix(IForgeRegistry<T> registry, T from, Ingredient ingredient, T to) {
            this(registry.getDelegateOrThrow(from), ingredient, registry.getDelegateOrThrow(to));
        }
    }

    private static final List<Mix<Potion>> ADDED_POTION_MIXES = new CopyOnWriteArrayList<>();
    private static final List<Mix<Item>> ADDED_CONTAINER_MIXES = new CopyOnWriteArrayList<>();
    private static final List<Ingredient> ADDED_CONTAINERS = new CopyOnWriteArrayList<>();
    private static final List<IBrewingRecipe> ADDED_RECIPES = new CopyOnWriteArrayList<>();
    /** {@code addMix(from, ingredient, to)} calls: replayed as the builder's own {@code addMix}. */
    private static final List<Object[]> SIMPLE_MIXES = new CopyOnWriteArrayList<>();
    private static boolean hooked;

    private static synchronized void hook() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(RegisterBrewingRecipesEvent.class, LegacyBrewing::replay);
    }

    /** Puts everything the mods added into the builder of one brewing registry build. */
    private static void replay(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();
        for (Object[] m : SIMPLE_MIXES) builder.addMix(BuiltInRegistries.POTION.wrapAsHolder((Potion) m[0]), (Item) m[1], BuiltInRegistries.POTION.wrapAsHolder((Potion) m[2]));
        for (Ingredient container : ADDED_CONTAINERS) {
            for (ItemStack s : container.getItems()) {
                try {
                    builder.addContainer(s.getItem());
                } catch (RuntimeException ignored) {
                    // not a potion item: not a container
                }
            }
        }
        for (Mix<Potion> m : ADDED_POTION_MIXES) builder.addRecipe(new PotionMix(m));
        for (Mix<Item> m : ADDED_CONTAINER_MIXES) builder.addRecipe(new ContainerMix(m));
        for (IBrewingRecipe r : ADDED_RECIPES) builder.addRecipe(r);
    }

    /** A potion mix as a recipe: an item holding the "from" potion plus the ingredient gives the same kind of item holding the "to" potion. */
    private record PotionMix(Mix<Potion> mix) implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            PotionContents contents = input.get(DataComponents.POTION_CONTENTS);
            return contents != null && contents.potion().map(p -> p.is(mix.from.key())).orElse(false);
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return mix.ingredient.test(ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            return isInput(input) && isIngredient(ingredient) ? PotionContents.createItemStack(input.getItem(), mix.to) : ItemStack.EMPTY;
        }
    }

    /** A container mix: a bottle of the "from" item becomes the "to" item with its potion contents kept. */
    private record ContainerMix(Mix<Item> mix) implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            return input.getItem() == mix.from.value();
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return mix.ingredient.test(ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            return isInput(input) && isIngredient(ingredient) ? input.transmuteCopy(mix.to.value()) : ItemStack.EMPTY;
        }
    }

    // ---- reads ------------------------------------------------------------------------------------------------------------------------------

    /** The brewing registry in use: the running server's, else the client level's (null before either exists). */
    private static PotionBrewing current() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.potionBrewing();
        return FMLEnvironment.dist.isClient() ? ClientBrewing.current() : null;
    }

    /** Client-only lookup, kept out of the common class so a dedicated server never loads the client classes. */
    private static final class ClientBrewing {
        static PotionBrewing current() {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            return level == null ? null : level.potionBrewing();
        }
    }

    private static <T> Holder.Reference<T> reference(Holder<T> holder, net.minecraft.core.Registry<T> registry) {
        if (holder instanceof Holder.Reference<T> r) return r;
        return holder.unwrapKey().flatMap(registry::getHolder).orElse(null);
    }

    private static List<Mix<Potion>> vanillaPotionMixes() {
        PotionBrewing b = current();
        List<Mix<Potion>> out = new ArrayList<>();
        if (b == null) return out;
        for (PotionBrewing.Mix<Potion> m : b.potionMixes) {
            Holder.Reference<Potion> from = reference(m.from(), BuiltInRegistries.POTION);
            Holder.Reference<Potion> to = reference(m.to(), BuiltInRegistries.POTION);
            if (from != null && to != null) out.add(new Mix<>(from, m.ingredient(), to));
        }
        return out;
    }

    private static List<Mix<Item>> vanillaContainerMixes() {
        PotionBrewing b = current();
        List<Mix<Item>> out = new ArrayList<>();
        if (b == null) return out;
        for (PotionBrewing.Mix<Item> m : b.containerMixes) {
            Holder.Reference<Item> from = reference(m.from(), BuiltInRegistries.ITEM);
            Holder.Reference<Item> to = reference(m.to(), BuiltInRegistries.ITEM);
            if (from != null && to != null) out.add(new Mix<>(from, m.ingredient(), to));
        }
        return out;
    }

    /** A list that reads vanilla's entries followed by the added ones and appends to the added ones. */
    private abstract static class ViewList<E> extends AbstractList<E> {
        abstract List<E> vanilla();

        abstract List<E> added();

        private List<E> all() {
            List<E> out = new ArrayList<>(vanilla());
            out.addAll(added());
            return out;
        }

        @Override
        public E get(int index) {
            return all().get(index);
        }

        @Override
        public int size() {
            return vanilla().size() + added().size();
        }

        @Override
        public boolean add(E e) {
            added().add(e);
            hook();
            return true;
        }

        @Override
        public void add(int index, E e) {
            add(e);
        }

        @Override
        public boolean remove(Object o) {
            return added().remove(o);
        }

        @Override
        public void clear() {
            added().clear();
        }
    }

    private static final List<Mix<Potion>> POTION_MIXES = new ViewList<>() {
        @Override
        List<Mix<Potion>> vanilla() {
            return vanillaPotionMixes();
        }

        @Override
        List<Mix<Potion>> added() {
            return ADDED_POTION_MIXES;
        }
    };

    private static final List<Mix<Item>> CONTAINER_MIXES = new ViewList<>() {
        @Override
        List<Mix<Item>> vanilla() {
            return vanillaContainerMixes();
        }

        @Override
        List<Mix<Item>> added() {
            return ADDED_CONTAINER_MIXES;
        }
    };

    private static final List<Ingredient> ALLOWED_CONTAINERS = new ViewList<>() {
        @Override
        List<Ingredient> vanilla() {
            PotionBrewing b = current();
            return b == null ? List.of() : b.containers;
        }

        @Override
        List<Ingredient> added() {
            return ADDED_CONTAINERS;
        }
    };

    /** {@code PotionBrewing.POTION_MIXES}. */
    public static List<?> potionMixes() {
        return POTION_MIXES;
    }

    /** {@code PotionBrewing.CONTAINER_MIXES}. */
    public static List<?> containerMixes() {
        return CONTAINER_MIXES;
    }

    /** {@code PotionBrewing.ALLOWED_CONTAINERS}. */
    public static List<?> allowedContainers() {
        return ALLOWED_CONTAINERS;
    }

    /** {@code PotionBrewing.ALLOWED_CONTAINER}: any stack matching one of the containers. */
    public static Predicate<ItemStack> allowedContainer() {
        return stack -> {
            for (Ingredient i : ALLOWED_CONTAINERS) if (i.test(stack)) return true;
            return false;
        };
    }

    /** {@code PotionBrewing.isBrewablePotion(potion)} (static in 1.20.1). */
    public static boolean isBrewablePotion(Potion potion) {
        PotionBrewing b = current();
        return b != null && b.isBrewablePotion(BuiltInRegistries.POTION.wrapAsHolder(potion));
    }

    // ---- writes -----------------------------------------------------------------------------------------------------------------------------

    /** {@code PotionBrewing.addMix(from, ingredient, to)}. */
    public static void addPotionMix(Potion from, Item ingredient, Potion to) {
        SIMPLE_MIXES.add(new Object[] {from, ingredient, to});
        hook();
    }

    /** {@code BrewingRecipeRegistry.addRecipe(recipe)}: a recipe implementing the NeoForge interface (the 1.20.1 one has the same three methods). */
    public static boolean addRecipe(Object recipe) {
        if (!(recipe instanceof IBrewingRecipe r)) return false;
        ADDED_RECIPES.add(r);
        hook();
        return true;
    }

    /** {@code BrewingRecipeRegistry.addRecipe(input, ingredient, output)}. */
    public static boolean addRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
        return addRecipe(new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack stack) {
                return input.test(stack);
            }

            @Override
            public boolean isIngredient(ItemStack stack) {
                return ingredient.test(stack);
            }

            @Override
            public ItemStack getOutput(ItemStack in, ItemStack ing) {
                return isInput(in) && isIngredient(ing) ? output.copy() : ItemStack.EMPTY;
            }
        });
    }

    /** {@code BrewingRecipeRegistry.getRecipes()}: the recipes of the brewing registry in use. */
    public static List<IBrewingRecipe> recipes() {
        PotionBrewing b = current();
        return b == null ? new ArrayList<>(ADDED_RECIPES) : b.getRecipes(); // the built registry already holds the replayed ones
    }

    /** {@code BrewingRecipeRegistry.canBrew(inputs, ingredient, slots)}: some slot holds an input that a recipe turns into something with this ingredient. */
    public static boolean canBrew(net.minecraft.core.NonNullList<ItemStack> inputs, ItemStack ingredient, int[] slots) {
        if (ingredient.isEmpty()) return false;
        for (int slot : slots) {
            ItemStack input = inputs.get(slot);
            if (input.isEmpty()) continue;
            for (IBrewingRecipe r : recipes()) if (!r.getOutput(input, ingredient).isEmpty()) return true;
        }
        return false;
    }

    /** {@code BrewingRecipeRegistry.brewPotions(inputs, ingredient, slots)}: every brewable slot becomes the output of the first recipe that accepts it. */
    public static void brewPotions(net.minecraft.core.NonNullList<ItemStack> inputs, ItemStack ingredient, int[] slots) {
        for (int slot : slots) {
            ItemStack input = inputs.get(slot);
            for (IBrewingRecipe r : recipes()) {
                ItemStack out = r.getOutput(input, ingredient);
                if (!out.isEmpty()) {
                    inputs.set(slot, out);
                    break;
                }
            }
        }
    }
}
