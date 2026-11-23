package org.kylepitcock.legacyconnector.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The host game's own smelting recipes, read from the vanilla data pack (no recipe manager needed, so it works while mods load and
 * from any thread). A legacy {@code FurnaceRecipes} list is seeded with these so mod machines that copy the list once at load smelt what
 * the game added since the legacy version (raw ores, wood and stripped logs, glazed terracotta, ancient debris...). Tag ingredients
 * are expanded from the pack's own tag files, because vanilla tags are not bound while mods load.
 */
public final class ModernSmelting {
    private ModernSmelting() {}

    /** One input item (all of its damage values) smelting to a result. */
    public record Entry(Item input, ItemStack result, float experience) {}

    private static final class Holder {
        static final List<Entry> ENTRIES = read();
    }

    /** The recipes, input item distinct (the first recipe of an item wins). Never null. */
    public static List<Entry> entries() {
        return Holder.ENTRIES;
    }

    private static List<Entry> read() {
        List<Entry> out = new ArrayList<>();
        try {
            var pack = net.minecraft.server.packs.repository.ServerPacksSource.createVanillaPackSource();
            List<JsonObject> recipes = new ArrayList<>();
            pack.listResources(PackType.SERVER_DATA, "minecraft", "recipe", (id, supplier) -> {
                try (var in = new InputStreamReader(supplier.get(), StandardCharsets.UTF_8)) {
                    JsonElement json = JsonParser.parseReader(in);
                    if (json.isJsonObject() && "minecraft:smelting".equals(string(json.getAsJsonObject(), "type"))) recipes.add(json.getAsJsonObject());
                } catch (IOException | RuntimeException e) {
                    // an unreadable recipe is left out
                }
            });
            Map<ResourceLocation, JsonObject> tags = new HashMap<>();
            pack.listResources(PackType.SERVER_DATA, "minecraft", "tags/item", (id, supplier) -> {
                try (var in = new InputStreamReader(supplier.get(), StandardCharsets.UTF_8)) {
                    JsonElement json = JsonParser.parseReader(in);
                    if (json.isJsonObject()) {
                        String path = id.getPath(); // tags/item/<name>.json
                        tags.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.substring("tags/item/".length(), path.length() - ".json".length())), json.getAsJsonObject());
                    }
                } catch (IOException | RuntimeException e) {
                    // an unreadable tag is left out
                }
            });
            Set<Item> seen = new LinkedHashSet<>();
            for (JsonObject recipe : recipes) {
                JsonElement result = recipe.get("result");
                String outId = result == null ? null : result.isJsonPrimitive() ? result.getAsString()
                        : result.isJsonObject() ? string(result.getAsJsonObject(), "id") : null;
                Item outItem = item(outId);
                if (outItem == null) continue;
                int count = result.isJsonObject() && result.getAsJsonObject().has("count") ? result.getAsJsonObject().get("count").getAsInt() : 1;
                float xp = recipe.has("experience") ? recipe.get("experience").getAsFloat() : 0.0F;
                List<JsonElement> options = new ArrayList<>();
                JsonElement ingredient = recipe.get("ingredient");
                if (ingredient != null && ingredient.isJsonArray()) ingredient.getAsJsonArray().forEach(options::add);
                else if (ingredient != null) options.add(ingredient);
                for (JsonElement option : options) {
                    if (!option.isJsonObject()) continue;
                    List<Item> inputs = new ArrayList<>();
                    String item = string(option.getAsJsonObject(), "item");
                    if (item != null) {
                        Item i = item(item);
                        if (i != null) inputs.add(i);
                    }
                    String tag = string(option.getAsJsonObject(), "tag");
                    if (tag != null) {
                        ResourceLocation rl = ResourceLocation.tryParse(tag);
                        if (rl != null) expand(rl, tags, inputs, new LinkedHashSet<>());
                    }
                    for (Item in : inputs) {
                        if (seen.add(in)) out.add(new Entry(in, new ItemStack(outItem, count), xp));
                    }
                }
            }
        } catch (RuntimeException | LinkageError e) {
            // without the vanilla pack the legacy entries stay
        }
        return List.copyOf(out);
    }

    private static void expand(ResourceLocation tag, Map<ResourceLocation, JsonObject> tags, List<Item> into, Set<ResourceLocation> visiting) {
        JsonObject json = tags.get(tag);
        if (json == null || !visiting.add(tag) || !json.has("values") || !json.get("values").isJsonArray()) return;
        for (JsonElement v : json.getAsJsonArray("values")) {
            String id = v.isJsonPrimitive() ? v.getAsString() : v.isJsonObject() ? string(v.getAsJsonObject(), "id") : null;
            if (id == null) continue;
            if (id.startsWith("#")) {
                ResourceLocation nested = ResourceLocation.tryParse(id.substring(1));
                if (nested != null) expand(nested, tags, into, visiting);
            } else {
                Item i = item(id);
                if (i != null) into.add(i);
            }
        }
    }

    private static Item item(String id) {
        if (id == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? null : item;
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }
}
