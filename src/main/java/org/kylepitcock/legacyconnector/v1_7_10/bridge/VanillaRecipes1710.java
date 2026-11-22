package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.ShapedRecipes;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.ShapelessRecipes;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.ShapedOreRecipe;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10's {@code CraftingManager.getRecipeList()} held the vanilla recipes; here they live in the modern recipe manager. Mods that read the list to
 * derive something from it (an EMC value for every item that can be crafted, an aspect for every recipe result, a recipe viewer's pages) saw only
 * the recipes legacy mods registered, so the items that stem from vanilla crafting got nothing. Once a server runs, the vanilla crafting recipes
 * are offered as the legacy recipe objects Forge's list held ({@code ShapedRecipes}, {@code ShapelessRecipes}, and the ore-dictionary kinds
 * for an ingredient that accepts several items, as a tag does). The views are read-only data: they are never registered (the game already has the
 * recipes) and never matched.
 */
public final class VanillaRecipes1710 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Recipes");
    private static volatile Object cachedFor;
    private static volatile List<IRecipe> cached = List.of();

    private VanillaRecipes1710() {}

    /** The vanilla recipes as legacy recipe objects (empty until a server's recipe manager holds them). */
    public static List<IRecipe> views() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return List.of();
        var manager = server.getRecipeManager();
        Object key = manager;
        if (cachedFor == key) return cached;
        synchronized (VanillaRecipes1710.class) {
            if (cachedFor == key) return cached;
            List<IRecipe> out = new ArrayList<>();
            int failed = 0;
            for (RecipeHolder<CraftingRecipe> holder : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
                if (!"minecraft".equals(holder.id().getNamespace())) continue;
                try {
                    IRecipe view = view(holder.value(), server.registryAccess());
                    if (view != null) out.add(view);
                } catch (RuntimeException | LinkageError e) {
                    failed++;
                }
            }
            cached = List.copyOf(out);
            cachedFor = key;
            LOG.info("Offering {} vanilla crafting recipes to legacy mods through CraftingManager.getRecipeList() ({} not convertible)", out.size(), failed);
            return cached;
        }
    }

    private static IRecipe view(CraftingRecipe recipe, net.minecraft.core.HolderLookup.Provider access) {
        net.minecraft.world.item.ItemStack result = recipe.getResultItem(access);
        if (result == null || result.isEmpty()) return null;
        ItemStack output = ItemStack.mcc$wrap(result.copy());
        if (output == null) return null;
        if (recipe instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth();
            int h = shaped.getHeight();
            Object[] cells = new Object[w * h];
            boolean simple = true;
            for (int i = 0; i < cells.length; i++) {
                cells[i] = cell(shaped.getIngredients().get(i));
                if (cells[i] instanceof List) simple = false;
            }
            if (simple) {
                ItemStack[] items = new ItemStack[cells.length];
                for (int i = 0; i < items.length; i++) items[i] = (ItemStack) cells[i];
                return new ShapedRecipes(w, h, items, output);
            }
            List<Object> args = new ArrayList<>();
            for (int y = 0; y < h; y++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < w; x++) row.append(cells[y * w + x] == null ? ' ' : (char) ('a' + y * w + x));
                args.add(row.toString());
            }
            for (int i = 0; i < cells.length; i++) {
                if (cells[i] == null) continue;
                args.add((char) ('a' + i));
                args.add(cells[i]);
            }
            return new ShapedOreRecipe(output, args.toArray());
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            List<Object> cells = new ArrayList<>();
            boolean simple = true;
            for (Ingredient ing : shapeless.getIngredients()) {
                Object cell = cell(ing);
                if (cell == null) continue;
                if (cell instanceof List) simple = false;
                cells.add(cell);
            }
            if (simple) {
                List<ItemStack> items = new ArrayList<>();
                for (Object c : cells) items.add((ItemStack) c);
                return new ShapelessRecipes(output, items);
            }
            return new ShapelessOreRecipe(output, cells.toArray());
        }
        return null;
    }

    /** An ingredient as Forge's recipes held it: one stack, or the list of stacks it accepts; null for an empty cell. */
    private static Object cell(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return null;
        net.minecraft.world.item.ItemStack[] options = ingredient.getItems();
        if (options.length == 0) return null;
        List<ItemStack> stacks = new ArrayList<>();
        java.util.Set<Object> seen = new java.util.HashSet<>();
        for (net.minecraft.world.item.ItemStack option : options) {
            if (option.isEmpty() || !seen.add(option.getItem())) continue;
            ItemStack one = ItemStack.mcc$wrap(option.copyWithCount(1));
            if (one != null) stacks.add(one);
        }
        if (stacks.isEmpty()) return null;
        return stacks.size() == 1 ? stacks.get(0) : stacks;
    }
}
