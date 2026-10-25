package org.kylepitcock.legacyconnector.v1_12_2.bridge;

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
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.ShapedRecipes;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.ShapelessRecipes;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.OreDictionary;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.OreIngredient;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.OreDictBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Recipes registered by 1.12.2 code (shaped, shapeless, ore dictionary and furnace) become data pack recipes. */
public final class RecipeBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Recipes");
    private static final Gson GSON = new Gson();
    private static final List<IRecipe> RECIPES = new ArrayList<>();
    private static final List<Object[]> SMELTING = new ArrayList<>();
    private static final Set<String> TAG_NAMES = new LinkedHashSet<>();

    private RecipeBridge1122() {}

    public static synchronized void add(IRecipe recipe) {
        RECIPES.add(recipe);
    }

    /** The result a mod registered for a smelting input (empty when none; vanilla's own smelting recipes are not listed here). */
    public static synchronized Object[] smeltingFor(ItemStack stack) {
        for (Object[] s : SMELTING) {
            ItemStack in = (ItemStack) s[0];
            if (in.getItem() == stack.getItem() && (in.getMetadata() == 32767 || in.getMetadata() == stack.getMetadata())) return s;
        }
        return null;
    }

    public static synchronized void addSmelting(ItemStack input, ItemStack output, float xp) {
        SMELTING.add(new Object[] {input, output, xp});
    }

    public static synchronized void generate(Map<String, Supplier<byte[]>> out) {
        TAG_NAMES.clear();
        int n = 0;
        for (IRecipe r : RECIPES) {
            try {
                JsonObject json = r instanceof ShapedRecipes s ? shaped(s) : r instanceof ShapelessRecipes s ? shapeless(s) : null;
                if (json == null) {
                    if (!(r instanceof ShapedRecipes) && !(r instanceof ShapelessRecipes)) LOG.debug("Recipe class {} is not bridged yet", r.getClass().getName());
                    continue;
                }
                var name = r.getRegistryName();
                String ns = Ids.modernModId(name.getNamespace());
                String path = Ids.normalizePath(name.getPath()).replace('/', '_') + "_code";
                byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
                out.put("data/" + ns + "/recipe/" + path + ".json", () -> bytes);
            } catch (RuntimeException e) {
                LOG.warn("Could not translate legacy recipe {}: {}", r.getRegistryName(), e.toString());
            }
        }
        for (Object[] s : SMELTING) {
            try {
                JsonObject o = new JsonObject();
                o.addProperty("type", "minecraft:smelting");
                o.addProperty("category", "misc");
                JsonElement ing = ingredient(Ingredient.fromStacks((ItemStack) s[0]));
                if (ing == null) continue;
                o.add("ingredient", ing);
                o.add("result", result((ItemStack) s[1]));
                o.addProperty("experience", (Float) s[2]);
                o.addProperty("cookingtime", 200);
                byte[] bytes = GSON.toJson(o).getBytes(StandardCharsets.UTF_8);
                out.put("data/legacyconnector/recipe/smelting_1122_" + (n++) + ".json", () -> bytes);
            } catch (RuntimeException e) {
                LOG.warn("Could not translate a legacy furnace recipe: {}", e.toString());
            }
        }
        generateTags(out);
    }

    private static JsonObject shaped(ShapedRecipes s) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:crafting_shaped");
        o.addProperty("category", "misc");
        Map<String, Character> letters = new LinkedHashMap<>();
        JsonObject key = new JsonObject();
        JsonArray pattern = new JsonArray();
        char next = 'A';
        for (int y = 0; y < s.recipeHeight; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < s.recipeWidth; x++) {
                Ingredient ing = s.recipeItems.get(x + y * s.recipeWidth);
                JsonElement j = ingredient(ing);
                if (j == null) {
                    if (ing != Ingredient.EMPTY && ing.getMatchingStacks().length > 0) return null; // an ingredient that cannot be expressed
                    row.append(' ');
                    continue;
                }
                String text = j.toString();
                Character c = letters.get(text);
                if (c == null) {
                    c = next++;
                    letters.put(text, c);
                    key.add(String.valueOf(c), j);
                }
                row.append(c);
            }
            pattern.add(row.toString());
        }
        if (key.size() == 0) return null;
        o.add("pattern", pattern);
        o.add("key", key);
        o.add("result", result(s.getRecipeOutput()));
        return o;
    }

    private static JsonObject shapeless(ShapelessRecipes s) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:crafting_shapeless");
        o.addProperty("category", "misc");
        JsonArray ings = new JsonArray();
        for (Ingredient in : s.recipeItems) {
            JsonElement j = ingredient(in);
            if (j == null) return null;
            ings.add(j);
        }
        o.add("ingredients", ings);
        o.add("result", result(s.getRecipeOutput()));
        return o;
    }

    private static JsonElement result(ItemStack output) {
        net.minecraft.world.item.ItemStack modern = Stacks1122.toModern(output).copy();
        if (modern.isEmpty()) throw new IllegalArgumentException("empty result");
        if (modern.getCount() > modern.getMaxStackSize()) modern.setCount(modern.getMaxStackSize());
        var ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        return net.minecraft.world.item.ItemStack.CODEC.encodeStart(ops, modern).getOrThrow();
    }

    /** Ingredient JSON, or null for an empty or inexpressible ingredient. */
    private static JsonElement ingredient(Ingredient ing) {
        if (ing == null || ing == Ingredient.EMPTY) return null;
        if (ing instanceof OreIngredient ore) return tag(ore.mcc$ore());
        if (ing.mcc$source instanceof String name) return tag(name);
        Set<String> ids = new LinkedHashSet<>();
        for (ItemStack stack : ing.getMatchingStacks()) {
            if (stack.isEmpty()) continue;
            if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE) {
                var base = ItemBridge1122.modern(stack.getItem(), 0);
                if (base != null) ids.add(BuiltInRegistries.ITEM.getKey(base).toString());
                for (var v : variants(stack)) ids.add(v);
            } else {
                var modern = Stacks1122.toModern(stack);
                if (!modern.isEmpty()) ids.add(BuiltInRegistries.ITEM.getKey(modern.getItem()).toString());
            }
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

    private static List<String> variants(ItemStack stack) {
        List<String> out = new ArrayList<>();
        for (int meta = 1; meta < 16; meta++) {
            var m = ItemBridge1122.modern(stack.getItem(), meta);
            var base = ItemBridge1122.modern(stack.getItem(), 0);
            if (m != null && m != base) out.add(BuiltInRegistries.ITEM.getKey(m).toString());
        }
        return out;
    }

    private static JsonElement tag(String oreName) {
        TAG_NAMES.add(oreName);
        JsonObject j = new JsonObject();
        j.addProperty("tag", OreDictBridge.tagFor(oreName).toString());
        return j;
    }

    private static void generateTags(Map<String, Supplier<byte[]>> out) {
        Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (var e : OreDictionary.mcc$registrations().entrySet()) {
            Set<String> ids = tags.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>());
            for (ItemStack s : e.getValue()) {
                JsonElement ing = ingredient(Ingredient.fromStacks(s));
                if (ing == null) continue;
                if (ing.isJsonArray()) for (JsonElement x : ing.getAsJsonArray()) ids.add(x.getAsJsonObject().get("item").getAsString());
                else if (ing.getAsJsonObject().has("item")) ids.add(ing.getAsJsonObject().get("item").getAsString());
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
}
