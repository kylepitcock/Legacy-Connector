package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code ModLoader.AddRecipe / AddShapelessRecipe / AddSmelting} as data pack recipes. Ingredients follow b1.7.3's CraftingManager: an
 * {@code Item} is that item with damage 0, a {@code Block} any metadata (here: the tag or the list of its variants, e.g. any wool), an
 * {@code ItemStack} its exact damage unless it is -1. Recipes are resolved when the pack is built, after the content was registered.
 */
public final class RecipesB173 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private record Recipe(String kind, ItemStack output, Object[] inputs, int smeltInput, LegacyModRecord mod) {}

    private static final List<Recipe> RECIPES = new ArrayList<>();
    private static int generated;
    private static int failed;

    private RecipesB173() {}

    public static synchronized void shaped(ItemStack output, Object[] inputs) {
        RECIPES.add(new Recipe("shaped", output == null ? null : output.copy(), inputs == null ? new Object[0] : inputs.clone(), 0, ContentB173.current()));
    }

    public static synchronized void shapeless(ItemStack output, Object[] inputs) {
        RECIPES.add(new Recipe("shapeless", output == null ? null : output.copy(), inputs == null ? new Object[0] : inputs.clone(), 0, ContentB173.current()));
    }

    public static synchronized void smelting(int input, ItemStack output) {
        RECIPES.add(new Recipe("smelting", output == null ? null : output.copy(), new Object[0], input, ContentB173.current()));
    }

    public static synchronized int size() {
        return RECIPES.size();
    }

    // ------------------------------------------------------------------ lookups for mods' own crafting tables and furnaces

    private static final java.util.concurrent.ConcurrentHashMap<Integer, Object> SMELTED = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile Object smeltedFor;
    private static final Object NONE = new Object();

    private static net.minecraft.server.MinecraftServer server() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    /**
     * {@code FurnaceRecipes.getSmeltingResult(id)} for an id no mod added a recipe for: the modern smelting recipe of the legacy item, as a
     * legacy stack (cached per id while the recipe manager stays the same). Null without a server (a remote client) or recipe.
     */
    public static ItemStack smeltResult(int id) {
        net.minecraft.server.MinecraftServer server = server();
        if (server == null) return null;
        var manager = server.getRecipeManager();
        if (smeltedFor != manager) {
            SMELTED.clear();
            smeltedFor = manager;
        }
        Object hit = SMELTED.computeIfAbsent(id, i -> {
            net.minecraft.world.item.ItemStack in = StacksB173.toModern(i, 1, 0);
            if (in.isEmpty()) return NONE;
            var input = new net.minecraft.world.item.crafting.SingleRecipeInput(in);
            ItemStack out = manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, server.overworld())
                    .map(r -> StacksB173.toLegacy(r.value().assemble(input, server.registryAccess())))
                    .orElse(null);
            return out == null ? NONE : out;
        });
        return hit instanceof ItemStack s ? s.copy() : null;
    }

    /** {@code CraftingManager.findMatchingRecipe}: the modern recipe manager's result for the grid, as a legacy stack. */
    public static ItemStack craft(legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingInventory grid) {
        net.minecraft.server.MinecraftServer server = server();
        if (server == null || grid == null || grid.width <= 0) return null;
        int size = grid.size();
        int height = size / grid.width;
        List<net.minecraft.world.item.ItemStack> items = new ArrayList<>(grid.width * height);
        boolean any = false;
        for (int i = 0; i < grid.width * height; i++) {
            ItemStack s = grid.getStack(i);
            net.minecraft.world.item.ItemStack m = StacksB173.toModern(s);
            if (s != null && m.isEmpty()) return null;
            any |= !m.isEmpty();
            items.add(m);
        }
        if (!any) return null;
        var input = net.minecraft.world.item.crafting.CraftingInput.of(grid.width, height, items);
        try {
            return server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, server.overworld())
                    .map(r -> StacksB173.toLegacy(r.value().assemble(input, server.registryAccess())))
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Recipe JSON files under {@code data/<mod>/recipe/b173_*}. */
    public static synchronized void generate(Map<String, Supplier<byte[]>> out) {
        generated = failed = 0;
        Set<String> used = new HashSet<>();
        for (Recipe r : RECIPES) {
            try {
                JsonObject json = switch (r.kind()) {
                    case "shaped" -> shapedJson(r);
                    case "shapeless" -> shapelessJson(r);
                    default -> smeltingJson(r);
                };
                if (json == null) {
                    failed++;
                    continue;
                }
                String ns = r.mod() != null ? r.mod().modernId : "legacyconnector";
                var result = BuiltInRegistries.ITEM.getKey(StacksB173.modernItem(r.output().itemId, r.output().getDamage()));
                String base = "b173_" + r.kind() + "/" + result.getNamespace() + "_" + result.getPath();
                String path = base;
                for (int k = 2; !used.add(ns + "/" + path); k++) path = base + "_" + k;
                byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
                out.put("data/" + ns + "/recipe/" + path + ".json", () -> bytes);
                generated++;
            } catch (RuntimeException e) {
                failed++;
                LOG.debug("b1.7.3 recipe for {} not translated: {}", r.output(), e.toString());
            }
        }
        if (!RECIPES.isEmpty()) LOG.info("b1.7.3 recipes: {} translated, {} not (unknown items)", generated, failed);
    }

    public static synchronized int[] counts() {
        return new int[] {generated, failed};
    }

    private static JsonObject shapedJson(Recipe r) {
        List<String> rows = new ArrayList<>();
        int i = 0;
        Object[] in = r.inputs();
        if (in.length > 0 && in[0] instanceof String[] array) {
            for (String s : array) rows.add(s);
            i = 1;
        } else {
            while (i < in.length && in[i] instanceof String s) {
                rows.add(s);
                i++;
            }
        }
        Map<Character, JsonElement> key = new LinkedHashMap<>();
        for (; i + 1 < in.length; i += 2) {
            if (!(in[i] instanceof Character c)) return null;
            JsonElement ing = ingredient(in[i + 1]);
            if (ing == null) return null;
            key.put(c, ing);
        }
        JsonArray pattern = new JsonArray();
        JsonObject keys = new JsonObject();
        for (String row : rows) {
            pattern.add(row);
            for (char c : row.toCharArray()) {
                if (c == ' ') continue;
                JsonElement ing = key.get(c);
                if (ing == null) return null;
                keys.add(String.valueOf(c), ing);
            }
        }
        if (rows.isEmpty()) return null;
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:crafting_shaped");
        o.addProperty("category", "misc");
        o.add("pattern", pattern);
        o.add("key", keys);
        JsonObject result = result(r.output());
        if (result == null) return null;
        o.add("result", result);
        return craftingResult(o, r);
    }

    private static JsonObject shapelessJson(Recipe r) {
        JsonArray ingredients = new JsonArray();
        for (Object input : r.inputs()) {
            JsonElement ing = ingredient(input);
            if (ing == null) return null;
            ingredients.add(ing);
        }
        if (ingredients.isEmpty()) return null;
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:crafting_shapeless");
        o.addProperty("category", "misc");
        o.add("ingredients", ingredients);
        JsonObject result = result(r.output());
        if (result == null) return null;
        o.add("result", result);
        return craftingResult(o, r);
    }

    private static JsonObject craftingResult(JsonObject recipe, Recipe legacy) {
        var stack = StacksB173.toModern(legacy.output());
        if (stack.getCount() <= stack.getMaxStackSize()) return recipe;
        recipe.getAsJsonObject("result").remove("count");
        recipe.remove("type");
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("type", "legacyconnector:oversized_" + legacy.kind());
        wrapper.add("recipe", recipe);
        wrapper.addProperty("count", stack.getCount());
        return wrapper;
    }

    private static JsonObject smeltingJson(Recipe r) {
        JsonElement ing = stackIngredient(r.smeltInput(), -1);
        JsonObject result = result(r.output());
        if (ing == null || result == null) return null;
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:smelting");
        o.addProperty("category", "misc");
        o.add("ingredient", ing);
        o.add("result", result);
        o.addProperty("experience", 0.1F);
        o.addProperty("cookingtime", 200);
        return o;
    }

    private static JsonElement ingredient(Object o) {
        if (o instanceof ItemStack s) return stackIngredient(s.itemId, s.getDamage());
        if (o instanceof Item i) return stackIngredient(i.id, 0);
        if (o instanceof Block b) return stackIngredient(b.id, -1);
        return null;
    }

    /** One item, or every variant of a vanilla id with metadata when any damage matches (-1). */
    private static JsonElement stackIngredient(int id, int damage) {
        if (damage == -1) {
            int variants = switch (id) {
                case 35, 351 -> 16;
                case 17, 18, 6 -> 3;
                case 44 -> 4;
                default -> 1;
            };
            if (variants > 1 && VanillaB173.isVanillaId(id) && (Item.ITEMS[id] == null || Item.ITEMS[id].mcc$owner == null)) {
                JsonArray any = new JsonArray();
                for (int d = 0; d < variants; d++) {
                    var item = VanillaB173.modernItem(id, d);
                    if (item != null) any.add(itemIngredient(item));
                }
                return any.isEmpty() ? null : any;
            }
            damage = 0;
        }
        var item = StacksB173.modernItem(id, damage);
        return item == null ? null : itemIngredient(item);
    }

    private static JsonObject itemIngredient(net.minecraft.world.item.Item item) {
        JsonObject o = new JsonObject();
        o.addProperty("item", BuiltInRegistries.ITEM.getKey(item).toString());
        return o;
    }

    private static JsonObject result(ItemStack legacy) {
        if (legacy == null) return null;
        var stack = StacksB173.toModern(legacy.itemId, Math.max(1, legacy.count), legacy.getDamage());
        if (stack.isEmpty()) return null;
        JsonObject o = new JsonObject();
        o.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getCount() > 1) o.addProperty("count", stack.getCount());
        var state = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        if (state != null && !state.isEmpty()) {
            JsonObject components = new JsonObject();
            JsonObject props = new JsonObject();
            state.properties().forEach(props::addProperty);
            components.add("minecraft:block_state", props);
            o.add("components", components);
        }
        return o;
    }
}
