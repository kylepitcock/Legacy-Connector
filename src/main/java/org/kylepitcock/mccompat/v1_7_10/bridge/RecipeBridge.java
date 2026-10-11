package org.kylepitcock.mccompat.v1_7_10.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.item.crafting.CraftingManager;
import mccompat.v1_7_10.net.minecraft.item.crafting.IRecipe;
import mccompat.v1_7_10.net.minecraft.item.crafting.ShapedRecipes;
import mccompat.v1_7_10.net.minecraft.item.crafting.ShapelessRecipes;
import mccompat.v1_7_10.net.minecraftforge.oredict.OreDictionary;
import mccompat.v1_7_10.net.minecraftforge.oredict.ShapedOreRecipe;
import mccompat.v1_7_10.net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Items;
import org.kylepitcock.mccompat.core.data.CompatComponents;
import org.kylepitcock.mccompat.v1_7_10.LegacyModContainer1710;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy code-registered recipes become ordinary data-pack recipes, generated into the compatibility data pack
 * every time data packs (re)load. Ingredient translation:
 * <ul>
 *     <li>item/block with wildcard metadata -> every flattened variant of it;</li>
 *     <li>specific metadata -> the flattened variant (or a component ingredient for unflattened metadata);</li>
 *     <li>ore dictionary name -> the equivalent item tag (see {@link OreDictBridge}).</li>
 * </ul>
 */
public final class RecipeBridge {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Recipes");
    private static final Gson GSON = new Gson();

    private sealed interface Spec permits Shaped, Shapeless, Smelting {}

    private record Shaped(String owner, ItemStack output, List<String> pattern, Map<Character, Object> key) implements Spec {}

    private record Shapeless(String owner, ItemStack output, List<Object> inputs) implements Spec {}

    private record Smelting(String owner, ItemStack input, ItemStack output, float xp) implements Spec {}

    private static final List<Spec> RECIPES = new ArrayList<>();
    private static final Set<String> TAG_NAMES = new LinkedHashSet<>();

    private RecipeBridge() {}

    private static String owner() {
        LegacyModContainer1710 c = LegacyModContainer1710.active();
        return c == null ? "retrofit" : c.record().modernId;
    }

    public static synchronized IRecipe addShaped(ItemStack output, Object[] params, boolean mirrored) {
        List<String> pattern = new ArrayList<>();
        Map<Character, Object> key = new LinkedHashMap<>();
        int i = 0;
        if (params.length > 0 && params[0] instanceof String[] rows) {
            pattern.addAll(List.of(rows));
            i = 1;
        } else {
            while (i < params.length && params[i] instanceof String row) {
                pattern.add(row);
                i++;
            }
        }
        for (; i + 1 < params.length; i += 2) {
            key.put((Character) params[i], params[i + 1]);
        }
        if (output == null || pattern.isEmpty()) {
            LOG.warn("Ignoring malformed legacy shaped recipe for {}", output);
        } else {
            RECIPES.add(new Shaped(owner(), output.copy(), pattern, key));
        }
        ShapedOreRecipe recipe = new ShapedOreRecipe(output, params);
        CraftingManager.getInstance().mcc$track(recipe);
        return recipe;
    }

    public static synchronized void addShapeless(ItemStack output, Object[] params) {
        if (output == null) return;
        RECIPES.add(new Shapeless(owner(), output.copy(), new ArrayList<>(List.of(params))));
        CraftingManager.getInstance().mcc$track(new ShapelessOreRecipe(output, params));
    }

    public static synchronized void addSmelting(ItemStack input, ItemStack output, float xp) {
        if (input == null || output == null) return;
        RECIPES.add(new Smelting(owner(), input.copy(), output.copy(), xp));
    }

    /** GameRegistry.addRecipe(IRecipe): listed in the legacy CraftingManager, which translates it. */
    public static void add(IRecipe recipe) {
        CraftingManager.getInstance().getRecipeList().add(recipe);
    }

    /** Translates a legacy recipe object into a data-pack recipe (called for everything added to the legacy list). */
    public static synchronized void addLegacyRecipe(IRecipe recipe) {
        if (recipe == null) return;
        if (recipe instanceof ShapedOreRecipe s) {
            addShapedSpec(s.getRecipeOutput(), s.mcc$args);
        } else if (recipe instanceof ShapelessOreRecipe s) {
            if (s.getRecipeOutput() != null) RECIPES.add(new Shapeless(owner(), s.getRecipeOutput().copy(), new ArrayList<>(List.of(s.mcc$args))));
        } else if (recipe instanceof ShapedRecipes s) {
            List<String> pattern = new ArrayList<>();
            Map<Character, Object> key = new LinkedHashMap<>();
            Map<String, Character> symbols = new LinkedHashMap<>();
            for (int y = 0; y < s.recipeHeight; y++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < s.recipeWidth; x++) {
                    ItemStack in = s.recipeItems[x + y * s.recipeWidth];
                    if (in == null) {
                        row.append(' ');
                        continue;
                    }
                    String id = in.getItem() + "@" + in.getItemDamage();
                    Character c = symbols.computeIfAbsent(id, k -> (char) ('A' + symbols.size()));
                    key.putIfAbsent(c, in);
                    row.append(c);
                }
                pattern.add(row.toString());
            }
            if (s.getRecipeOutput() != null) RECIPES.add(new Shaped(owner(), s.getRecipeOutput().copy(), pattern, key));
        } else if (recipe instanceof ShapelessRecipes s) {
            if (s.getRecipeOutput() != null) RECIPES.add(new Shapeless(owner(), s.getRecipeOutput().copy(), new ArrayList<Object>(s.recipeItems)));
        } else {
            LOG.warn("Custom legacy IRecipe {} is not supported yet and was skipped", recipe.getClass().getName());
        }
    }

    private static void addShapedSpec(ItemStack output, Object[] params) {
        List<String> pattern = new ArrayList<>();
        Map<Character, Object> key = new LinkedHashMap<>();
        int i = 0;
        if (params.length > 0 && params[0] instanceof String[] rows) {
            pattern.addAll(List.of(rows));
            i = 1;
        } else {
            while (i < params.length && params[i] instanceof String row) {
                pattern.add(row);
                i++;
            }
        }
        for (; i + 1 < params.length; i += 2) key.put((Character) params[i], params[i + 1]);
        if (output != null && !pattern.isEmpty()) RECIPES.add(new Shaped(owner(), output.copy(), pattern, key));
    }

    // ------------------------------------------------------------------ data pack generation

    /** Writes recipe JSON (and the item tags they and the ore dictionary need) into {@code out}. */
    public static synchronized void generate(Map<String, Supplier<byte[]>> out) {
        TAG_NAMES.clear();
        int n = 0;
        for (Spec spec : RECIPES) {
            try {
                JsonObject json = switch (spec) {
                    case Shaped s -> shaped(s);
                    case Shapeless s -> shapeless(s);
                    case Smelting s -> smelting(s);
                };
                if (json == null) continue;
                String ns = switch (spec) {
                    case Shaped s -> s.owner;
                    case Shapeless s -> s.owner;
                    case Smelting s -> s.owner;
                };
                byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
                out.put("data/" + ns + "/recipe/legacy_" + (n++) + ".json", () -> bytes);
            } catch (RuntimeException e) {
                LOG.warn("Could not translate legacy recipe {}: {}", spec, e.toString());
            }
        }
        generateTags(out);
    }

    private static JsonObject shaped(Shaped s) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:crafting_shaped");
        o.addProperty("category", "misc");
        JsonArray pattern = new JsonArray();
        int width = s.pattern.stream().mapToInt(String::length).max().orElse(0);
        for (String row : s.pattern) pattern.add(String.format("%-" + width + "s", row));
        o.add("pattern", pattern);
        // Legacy Forge ignored keys the pattern never uses (mods often pass one shared key list to every recipe);
        // 1.21 rejects the whole recipe, and an untranslatable unused ingredient must not sink it either.
        String used = String.join("", s.pattern);
        JsonObject key = new JsonObject();
        for (var e : s.key.entrySet()) {
            if (e.getKey() == ' ' || used.indexOf(e.getKey()) < 0) continue;
            JsonElement ing = ingredient(e.getValue());
            if (ing == null) return null;
            key.add(String.valueOf(e.getKey()), ing);
        }
        o.add("key", key);
        o.add("result", result(s.output));
        return o;
    }

    private static JsonObject shapeless(Shapeless s) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:crafting_shapeless");
        o.addProperty("category", "misc");
        JsonArray ings = new JsonArray();
        for (Object in : s.inputs) {
            JsonElement ing = ingredient(in);
            if (ing == null) return null;
            ings.add(ing);
        }
        o.add("ingredients", ings);
        o.add("result", result(s.output));
        return o;
    }

    private static JsonObject smelting(Smelting s) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:smelting");
        o.addProperty("category", "misc");
        JsonElement ing = ingredient(s.input);
        if (ing == null) return null;
        o.add("ingredient", ing);
        o.add("result", result(s.output));
        o.addProperty("experience", s.xp);
        o.addProperty("cookingtime", 200);
        return o;
    }

    /** Ingredient JSON, or null if it cannot be expressed (recipe is then skipped with a warning). */
    static JsonElement ingredient(Object o) {
        if (o instanceof String ore) {
            TAG_NAMES.add(ore);
            JsonObject j = new JsonObject();
            j.addProperty("tag", OreDictBridge.tagFor(ore).toString());
            return j;
        }
        if (o instanceof Item item) return ingredient(new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
        if (o instanceof Block block) return ingredient(new ItemStack(block, 1, OreDictionary.WILDCARD_VALUE));
        if (o instanceof List<?> list) {
            JsonArray arr = new JsonArray();
            for (Object e : list) {
                JsonElement j = ingredient(e);
                if (j != null) arr.add(j);
            }
            return arr.isEmpty() ? null : arr;
        }
        if (o instanceof ItemStack stack) {
            Set<String> ids = new LinkedHashSet<>();
            int meta = stack.getItemDamage();
            Item item = stack.getItem();
            if (item == null) return null;
            if (meta == OreDictionary.WILDCARD_VALUE) {
                for (int m : metasOf(item)) ids.add(itemId(new ItemStack(item, 1, m).mcc$modern()));
            } else {
                net.minecraft.world.item.ItemStack modern = stack.mcc$modern();
                Integer extra = modern.get(CompatComponents.legacyMeta());
                if (extra != null) {
                    // An item that did not translate is air: clients refuse recipes with empty stacks (disconnect).
                    if (itemId(modern).equals("minecraft:air")) return null;
                    JsonObject j = new JsonObject();
                    j.addProperty("type", "neoforge:components");
                    j.addProperty("items", itemId(modern));
                    JsonObject comps = new JsonObject();
                    comps.addProperty("retrofit:legacy_meta", extra);
                    j.add("components", comps);
                    return j;
                }
                ids.add(itemId(modern));
            }
            ids.remove("minecraft:air");
            if (ids.isEmpty()) return null;
            if (ids.size() == 1) {
                JsonObject j = new JsonObject();
                j.addProperty("item", ids.iterator().next());
                return j;
            }
            JsonArray arr = new JsonArray();
            for (String id : ids) {
                JsonObject j = new JsonObject();
                j.addProperty("item", id);
                arr.add(j);
            }
            return arr;
        }
        return null;
    }

    private static List<Integer> metasOf(Item item) {
        if (!item.mcc$variants.isEmpty()) return new ArrayList<>(item.mcc$variants.keySet());
        if (!item.getHasSubtypes()) return List.of(0);
        List<Integer> all = new ArrayList<>();
        for (int m = 0; m < 16; m++) all.add(m);
        return all;
    }

    private static String itemId(net.minecraft.world.item.ItemStack s) {
        if (s.isEmpty() || s.is(Items.BARRIER)) return "minecraft:air";
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    private static JsonElement result(ItemStack output) {
        net.minecraft.world.item.ItemStack modern = output.mcc$modern().copy();
        if (modern.isEmpty()) throw new IllegalArgumentException("empty result");
        var ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        return net.minecraft.world.item.ItemStack.CODEC.encodeStart(ops, modern).getOrThrow();
    }

    /** One item tag per ore dictionary name used by legacy registrations or recipes. */
    private static void generateTags(Map<String, Supplier<byte[]>> out) {
        Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (var e : OreDictionary.mcc$registrations().entrySet()) {
            Set<String> ids = tags.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>());
            for (ItemStack s : e.getValue()) {
                JsonElement ing = ingredient(s);
                collectIds(ing, ids);
            }
        }
        for (String name : TAG_NAMES) tags.computeIfAbsent(name, k -> new LinkedHashSet<>());
        for (var e : tags.entrySet()) {
            var tag = OreDictBridge.tagFor(e.getKey());
            JsonObject json = new JsonObject();
            json.addProperty("replace", false);
            JsonArray values = new JsonArray();
            for (String id : e.getValue()) {
                JsonObject v = new JsonObject();
                v.addProperty("id", id);
                v.addProperty("required", false);
                values.add(v);
            }
            json.add("values", values);
            byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
            out.merge("data/" + tag.getNamespace() + "/tags/item/" + tag.getPath() + ".json", () -> bytes, (a, b) -> a);
        }
    }

    private static void collectIds(JsonElement ing, Set<String> into) {
        collectAll(ing, into);
        into.remove("minecraft:air"); // a tag containing air is an empty stack on the client
    }

    private static void collectAll(JsonElement ing, Set<String> into) {
        if (ing == null) return;
        if (ing.isJsonArray()) for (JsonElement e : ing.getAsJsonArray()) collectAll(e, into);
        else if (ing.isJsonObject() && ing.getAsJsonObject().has("item")) into.add(ing.getAsJsonObject().get("item").getAsString());
        else if (ing.isJsonObject() && ing.getAsJsonObject().has("items")) into.add(ing.getAsJsonObject().get("items").getAsString());
    }

    public static synchronized int count() {
        return RECIPES.size();
    }
}
