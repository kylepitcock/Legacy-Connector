package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * The 1.20.1 JSON / network methods of the vanilla recipe serializers ({@code ShapedRecipe.Serializer.fromJson(id, json)} and so on), driven from the
 * 1.21.1 codec and stream codec of the same serializer, and the static JSON helpers of {@code ShapedRecipe} (see {@code mc-calls.tsv}). A mod serializer
 * that extends a vanilla one and overrides {@code fromJson} keeps its override: normal calls reach it through {@link RecipeCompat}, and a
 * {@code super.fromJson} call lands here and reads the vanilla form.
 */
public final class RecipeSerializerCompat {
    private RecipeSerializerCompat() {}

    // ------------------------------------------------------------------ the codec pair behind a serializer

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object readJson(RecipeSerializer serializer, JsonObject json) {
        com.mojang.serialization.DataResult<Object> r = serializer.codec().codec().parse(JsonOps.INSTANCE, legacyResult(json));
        return r.getOrThrow(msg -> new JsonParseException(String.valueOf(msg)));
    }

    /**
     * The 1.20.1 result forms read as the 1.21.1 item stack: {@code {"item": id, "count": n}} becomes {@code {"id": id, "count": n}}, and the bare item name
     * of a single-item recipe (with a sibling {@code "count"}) becomes the same object. NBT of a 1.20.1 result is carried as a custom data component.
     */
    static JsonObject legacyResult(JsonObject json) {
        JsonElement result = json.get("result");
        if (result == null) return json;
        JsonObject copy = json.deepCopy();
        if (result.isJsonPrimitive()) {
            JsonObject stack = new JsonObject();
            stack.addProperty("id", result.getAsString());
            if (json.has("count")) stack.add("count", json.get("count"));
            copy.add("result", stack);
        } else if (result.isJsonObject() && result.getAsJsonObject().has("item") && !result.getAsJsonObject().has("id")) {
            JsonObject stack = result.getAsJsonObject().deepCopy();
            stack.add("id", stack.remove("item"));
            if (stack.has("nbt")) {
                JsonObject components = new JsonObject();
                components.addProperty("minecraft:custom_data", stack.remove("nbt").getAsString());
                stack.add("components", components);
            }
            copy.add("result", stack);
        }
        return copy;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object readNetwork(RecipeSerializer serializer, FriendlyByteBuf buf) {
        RegistryFriendlyByteBuf reg = Round3Compat.registryBuffer(buf);
        int before = reg.readerIndex();
        Object recipe = ((StreamCodec) serializer.streamCodec()).decode(reg);
        // the bytes were copied when the buffer was not registry-aware: the caller's buffer has to move on too
        if (reg != buf) buf.readerIndex(buf.readerIndex() + (reg.readerIndex() - before));
        return recipe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void writeNetwork(RecipeSerializer serializer, FriendlyByteBuf buf, Object recipe) {
        if (buf instanceof RegistryFriendlyByteBuf reg) {
            ((StreamCodec) serializer.streamCodec()).encode(reg, recipe);
            return;
        }
        RegistryFriendlyByteBuf out = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), MigrateContext.registries());
        ((StreamCodec) serializer.streamCodec()).encode(out, recipe);
        buf.writeBytes(out);
    }

    // ------------------------------------------------------------------ ShapedRecipe.Serializer

    public static ShapedRecipe shapedFromJson(ShapedRecipe.Serializer s, ResourceLocation id, JsonObject json) {
        return (ShapedRecipe) readJson(s, json);
    }

    public static ShapedRecipe shapedFromNetwork(ShapedRecipe.Serializer s, ResourceLocation id, FriendlyByteBuf buf) {
        return (ShapedRecipe) readNetwork(s, buf);
    }

    public static void shapedToNetwork(ShapedRecipe.Serializer s, FriendlyByteBuf buf, ShapedRecipe recipe) {
        writeNetwork(s, buf, recipe);
    }

    // ------------------------------------------------------------------ ShapelessRecipe.Serializer

    public static ShapelessRecipe shapelessFromJson(ShapelessRecipe.Serializer s, ResourceLocation id, JsonObject json) {
        return (ShapelessRecipe) readJson(s, json);
    }

    public static ShapelessRecipe shapelessFromNetwork(ShapelessRecipe.Serializer s, ResourceLocation id, FriendlyByteBuf buf) {
        return (ShapelessRecipe) readNetwork(s, buf);
    }

    public static void shapelessToNetwork(ShapelessRecipe.Serializer s, FriendlyByteBuf buf, ShapelessRecipe recipe) {
        writeNetwork(s, buf, recipe);
    }

    // ------------------------------------------------------------------ SingleItemRecipe.Serializer

    public static SingleItemRecipe singleFromJson(SingleItemRecipe.Serializer<?> s, ResourceLocation id, JsonObject json) {
        return (SingleItemRecipe) readJson(s, json);
    }

    public static SingleItemRecipe singleFromNetwork(SingleItemRecipe.Serializer<?> s, ResourceLocation id, FriendlyByteBuf buf) {
        return (SingleItemRecipe) readNetwork(s, buf);
    }

    public static void singleToNetwork(SingleItemRecipe.Serializer<?> s, FriendlyByteBuf buf, SingleItemRecipe recipe) {
        writeNetwork(s, buf, recipe);
    }

    // ------------------------------------------------------------------ ShapedRecipe statics (1.20.1 JSON helpers, same rules as vanilla 1.20.1)

    /** {@code ShapedRecipe.setCraftingSize(width, height)}: moved to the pattern class. */
    public static void setCraftingSize(int width, int height) {
        ShapedRecipePattern.setCraftingSize(width, height);
    }

    /** {@code ShapedRecipe.itemFromJson(json)}: the item named by {@code "item"}. */
    public static Item itemFromJson(JsonObject json) {
        String name = GsonHelper.getAsString(json, "item");
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(name))
                .orElseThrow(() -> new JsonSyntaxException("Unknown item '" + name + "'"));
        if (item == net.minecraft.world.item.Items.AIR) throw new JsonSyntaxException("Invalid item: " + name);
        return item;
    }

    /** {@code ShapedRecipe.patternFromJson(array)}: the rows of the pattern, within the crafting grid size. */
    public static String[] patternFromJson(JsonArray array) {
        int maxW = ShapedRecipePattern.getMaxWidth();
        int maxH = ShapedRecipePattern.getMaxHeight();
        String[] rows = new String[array.size()];
        if (rows.length > maxH) throw new JsonSyntaxException("Invalid pattern: too many rows, " + maxH + " is maximum");
        if (rows.length == 0) throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        for (int i = 0; i < rows.length; i++) {
            String row = GsonHelper.convertToString(array.get(i), "pattern[" + i + "]");
            if (row.length() > maxW) throw new JsonSyntaxException("Invalid pattern: too many columns, " + maxW + " is maximum");
            if (i > 0 && rows[0].length() != row.length()) throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
            rows[i] = row;
        }
        return rows;
    }

    /** {@code ShapedRecipe.keyFromJson(json)}: one ingredient per single-character key, a space meaning empty. */
    public static Map<String, Ingredient> keyFromJson(JsonObject json) {
        Map<String, Ingredient> map = new java.util.HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getKey().length() != 1) throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            if (" ".equals(entry.getKey())) throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            map.put(entry.getKey(), MiscCompat.ingredientFromJson(entry.getValue()));
        }
        map.put(" ", Ingredient.EMPTY);
        return map;
    }

    /** {@code ShapedRecipe.shrink(pattern)}: removes empty leading and trailing rows and columns. */
    public static String[] shrink(String... pattern) {
        int firstCol = Integer.MAX_VALUE;
        int lastCol = 0;
        int leading = 0;
        int trailing = 0;
        for (int i = 0; i < pattern.length; i++) {
            String row = pattern[i];
            firstCol = Math.min(firstCol, firstNonSpace(row));
            int last = lastNonSpace(row);
            lastCol = Math.max(lastCol, last);
            if (last < 0) {
                if (leading == i) leading++;
                trailing++;
            } else {
                trailing = 0;
            }
        }
        if (pattern.length == trailing) return new String[0];
        String[] out = new String[pattern.length - trailing - leading];
        for (int i = 0; i < out.length; i++) out[i] = pattern[i + leading].substring(firstCol, lastCol + 1);
        return out;
    }

    private static int firstNonSpace(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }

    private static int lastNonSpace(String s) {
        int i = s.length() - 1;
        while (i >= 0 && s.charAt(i) == ' ') i--;
        return i;
    }

    /** {@code ShapedRecipe.dissolvePattern(pattern, keys, width, height)}: the ingredient grid of a pattern. */
    public static NonNullList<Ingredient> dissolvePattern(String[] pattern, Map<String, Ingredient> keys, int width, int height) {
        NonNullList<Ingredient> list = NonNullList.withSize(width * height, Ingredient.EMPTY);
        java.util.Set<String> unused = new java.util.HashSet<>(keys.keySet());
        unused.remove(" ");
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length(); x++) {
                String key = pattern[y].substring(x, x + 1);
                Ingredient ingredient = keys.get(key);
                if (ingredient == null) throw new JsonSyntaxException("Pattern references symbol '" + key + "' but it's not defined in the key");
                unused.remove(key);
                list.set(x + width * y, ingredient);
            }
        }
        if (!unused.isEmpty()) throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + unused);
        return list;
    }
}
