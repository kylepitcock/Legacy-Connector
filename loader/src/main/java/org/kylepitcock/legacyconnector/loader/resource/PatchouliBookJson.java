package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * The {@code book.json} of a Patchouli 1.12 book as the modern Patchouli reads it. The modern Patchouli finds the books of a mod by
 * scanning the mod's jar for {@code data/<mod>/patchouli_books/<book>/book.json}, so this one file has to be in the translated jar (the
 * rest of the book is read from the resource manager and converted by the runtime, which needs the registries; see the wiki's Bridges
 * page). Only what needs no registries is converted here: the same rules as the runtime's {@code PatchouliBooks.book}.
 */
public final class PatchouliBookJson {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** 1.12 creative tab labels to the modern tab ids (anything else, a mod's own tab, is dropped). */
    private static final Map<String, String> CREATIVE_TABS = Map.ofEntries(
            Map.entry("buildingblocks", "minecraft:building_blocks"),
            Map.entry("decorations", "minecraft:functional_blocks"),
            Map.entry("redstone", "minecraft:redstone_blocks"),
            Map.entry("transportation", "minecraft:redstone_blocks"),
            Map.entry("misc", "minecraft:tools_and_utilities"),
            Map.entry("search", "minecraft:tools_and_utilities"),
            Map.entry("food", "minecraft:food_and_drinks"),
            Map.entry("tools", "minecraft:tools_and_utilities"),
            Map.entry("combat", "minecraft:combat"),
            Map.entry("brewing", "minecraft:food_and_drinks"),
            Map.entry("materials", "minecraft:ingredients"));

    private PatchouliBookJson() {}

    /** {@code assets/<ns>/patchouli_books/<book>/book.json} (already normalized) as {@code data/<ns>/patchouli_books/<book>/book.json}, or null for any other path. */
    public static String dataPath(String normalizedAssetPath) {
        if (!normalizedAssetPath.matches("assets/[^/]+/patchouli_books/[^/]+/book\\.json")) return null;
        return "data/" + normalizedAssetPath.substring("assets/".length());
    }

    /** The converted json, or the input when it cannot be read. */
    public static byte[] convert(byte[] data) {
        JsonObject o;
        try {
            JsonReader reader = new JsonReader(new StringReader(new String(data, StandardCharsets.UTF_8).replace("﻿", "")));
            reader.setLenient(true);
            var root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return data;
            o = root.getAsJsonObject();
        } catch (RuntimeException e) {
            return data;
        }
        // Patchouli 1.20 removed books whose contents are server data: the contents stay in assets (as in 1.12), only book.json is data
        o.addProperty("use_resource_pack", true);
        for (String key : new String[] {"book_texture", "filler_texture", "crafting_texture", "open_sound", "flip_sound", "model"}) resource(o, key);
        if (o.has("custom_book_item") && o.get("custom_book_item").isJsonPrimitive()) {
            // "mod:item:meta" -> "mod:item" (the runtime does not rewrite book.json: Patchouli reads it from the jar)
            String[] p = o.get("custom_book_item").getAsString().split(":");
            o.addProperty("custom_book_item", p.length >= 2 ? LegacyResourceConverter.normalizePath(p[0]) + ":" + LegacyResourceConverter.normalizePath(p[1]) : p[0]);
        }
        if (o.has("creative_tab") && o.get("creative_tab").isJsonPrimitive()) {
            String raw = o.get("creative_tab").getAsString();
            String key = raw.toLowerCase(Locale.ROOT).replace("tab", "").replace(".", "").replace("_", "").trim();
            String mapped = raw.contains(":") ? raw : CREATIVE_TABS.get(key);
            if (mapped != null) o.addProperty("creative_tab", mapped);
            else o.remove("creative_tab");
        }
        return GSON.toJson(o).getBytes(StandardCharsets.UTF_8);
    }

    private static void resource(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive() || !o.get(key).getAsJsonPrimitive().isString()) return;
        String v = o.get(key).getAsString();
        int c = v.indexOf(':');
        o.addProperty(key, c < 0 ? LegacyResourceConverter.normalizePath(v)
                : LegacyResourceConverter.normalizePath(v.substring(0, c)) + ":" + LegacyResourceConverter.normalizePath(v.substring(c + 1)));
    }
}
