package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionHelper;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionType;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils;
import legacyconnector.v1_12_2.net.minecraftforge.common.brewing.BrewingRecipe;
import legacyconnector.v1_12_2.net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.common.brewing.IBrewingRecipe;
import legacyconnector.v1_12_2.net.minecraftforge.common.brewing.VanillaBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy brewing into the modern brewing stand: when the server builds its brewing table, every potion mix of {@link PotionHelper} and every
 * Forge {@link BrewingRecipeRegistry} recipe a 1.12.2 mod added becomes a modern mix or brewing recipe. Mixes between vanilla potion types are
 * plain modern mixes; everything else is matched through the legacy objects.
 */
public final class Brewing1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Brewing");
    private static boolean listening;

    private Brewing1122() {}

    public static synchronized void ensureListener() {
        if (listening) return;
        listening = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(RegisterBrewingRecipesEvent.class, Brewing1122::register);
    }

    private static void register(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        int mixes = 0, recipes = 0;
        // containers mods added to PotionHelper.POTION_ITEMS (the vanilla bottles are the modern defaults)
        for (var ingredient : java.util.List.copyOf(PotionHelper.POTION_ITEMS)) {
            try {
                for (ItemStack s : ingredient.getMatchingStacks()) {
                    var item = Stacks1122.toModern(s).getItem();
                    if (item != net.minecraft.world.item.Items.AIR && item != net.minecraft.world.item.Items.POTION && item != net.minecraft.world.item.Items.SPLASH_POTION
                            && item != net.minecraft.world.item.Items.LINGERING_POTION) {
                        builder.addContainer(item);
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Cannot add a legacy potion container: {}", e.toString());
            }
        }
        for (PotionHelper.MixPredicate<PotionType> mix : PotionHelper.mcc$mixes()) {
            try {
                var from = mix.mcc$input() == null ? null : mix.mcc$input().mcc$modern();
                var to = mix.mcc$output() == null ? null : mix.mcc$output().mcc$modern();
                ItemStack[] reagents = mix.reagent.getMatchingStacks();
                if (from != null && to != null && reagents.length > 0) {
                    java.util.Set<net.minecraft.world.item.Item> done = new java.util.HashSet<>();
                    for (ItemStack r : reagents) {
                        var item = Stacks1122.toModern(r).getItem();
                        if (item != net.minecraft.world.item.Items.AIR && done.add(item)) builder.addMix(from, item, to);
                    }
                } else {
                    builder.addRecipe(new LegacyMix(mix));
                }
                mixes++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Cannot add a legacy potion mix: {}", e.toString());
            }
        }
        for (IBrewingRecipe recipe : BrewingRecipeRegistry.getRecipes()) {
            if (recipe instanceof VanillaBrewingRecipe) continue;
            try {
                if (recipe instanceof BrewingRecipe simple && simple.getInput().getMetadata() != 32767 && simple.getIngredient().getMetadata() != 32767) {
                    // stack to stack: a plain modern recipe (recipe viewers show it); potion inputs match on their components
                    var input = Stacks1122.toModern(simple.getInput());
                    var ingredient = Stacks1122.toModern(simple.getIngredient());
                    var output = Stacks1122.toModern(simple.getOutput());
                    if (!input.isEmpty() && !ingredient.isEmpty() && !output.isEmpty()) {
                        builder.addRecipe(net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false, input),
                                net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false, ingredient), output);
                        recipes++;
                        continue;
                    }
                }
                builder.addRecipe(new LegacyRecipe(recipe));
                recipes++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Cannot add a legacy brewing recipe {}: {}", recipe.getClass().getName(), e.toString());
            }
        }
        if (mixes + recipes > 0) LOG.info("Added {} legacy potion mixes and {} legacy brewing recipes to the brewing stand", mixes, recipes);
    }

    /** A potion mix involving a type with no modern potion: matched on the legacy stacks. */
    private record LegacyMix(PotionHelper.MixPredicate<PotionType> mix) implements net.neoforged.neoforge.common.brewing.IBrewingRecipe {
        @Override
        public boolean isInput(net.minecraft.world.item.ItemStack input) {
            return !input.isEmpty() && PotionUtils.getPotionFromItem(Stacks1122.fromModern(input)) == mix.mcc$input();
        }

        @Override
        public boolean isIngredient(net.minecraft.world.item.ItemStack ingredient) {
            return !ingredient.isEmpty() && mix.reagent.apply(Stacks1122.fromModern(ingredient));
        }

        @Override
        public net.minecraft.world.item.ItemStack getOutput(net.minecraft.world.item.ItemStack input, net.minecraft.world.item.ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) return net.minecraft.world.item.ItemStack.EMPTY;
            return Stacks1122.toModern(PotionUtils.addPotionToItemStack(Stacks1122.fromModern(input).copy(), mix.mcc$output()));
        }
    }

    /** A Forge brewing recipe of a legacy mod, asked through legacy stacks. */
    private record LegacyRecipe(IBrewingRecipe recipe) implements net.neoforged.neoforge.common.brewing.IBrewingRecipe {
        @Override
        public boolean isInput(net.minecraft.world.item.ItemStack input) {
            return !input.isEmpty() && recipe.isInput(Stacks1122.fromModern(input));
        }

        @Override
        public boolean isIngredient(net.minecraft.world.item.ItemStack ingredient) {
            return !ingredient.isEmpty() && recipe.isIngredient(Stacks1122.fromModern(ingredient));
        }

        @Override
        public net.minecraft.world.item.ItemStack getOutput(net.minecraft.world.item.ItemStack input, net.minecraft.world.item.ItemStack ingredient) {
            if (input.isEmpty() || ingredient.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
            return Stacks1122.toModern(recipe.getOutput(Stacks1122.fromModern(input), Stacks1122.fromModern(ingredient)));
        }
    }
}
