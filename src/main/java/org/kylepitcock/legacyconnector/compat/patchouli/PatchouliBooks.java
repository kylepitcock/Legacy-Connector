package org.kylepitcock.legacyconnector.compat.patchouli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts the json of a Patchouli 1.12 book (what legacy mods ship under {@code assets/<mod>/patchouli_books/<book>/}) into what the modern
 * Patchouli reads from {@code data/<mod>/patchouli_books/<book>/}. The structure is the same; what changed is how things are named:
 * <ul>
 *     <li>items are {@code mod:item:meta} in 1.12 (the layer flattens subtypes into items of their own), blocks are {@code mod:block[prop=value]}
 *     (the layer stores a legacy block's state as one {@code meta} property), recipes are the legacy recipe names (the layer's generated
 *     recipes carry a {@code _code} suffix), furnace pages name the input item and not the recipe;</li>
 *     <li>resource ids are lower case without punctuation in the modern namespace rules;</li>
 *     <li>{@code creative_tab} names a 1.12 creative tab.</li>
 * </ul>
 * The id mapping is a {@link Resolver} so the conversion is testable without the game.
 */
public final class PatchouliBooks {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Set<String> BUILTIN_PAGES = Set.of("text", "spotlight", "crafting", "smelting", "image", "multiblock", "entity", "link",
            "relations", "quest", "empty");

    /** 1.12 creative tab labels to the modern tab ids. */
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

    /** The id mapping the conversion needs from the layer. */
    public interface Resolver {
        /** A 1.12 item string ({@code mod:item[:meta][#count][{nbt}]}) as a modern one; the input when unknown. */
        String item(String spec);

        /** A 1.12 block string as a modern one; the input when unknown. */
        String block(String spec);

        /** A 1.12 entity id as a modern one. */
        String entity(String id);

        /** A 1.12 crafting recipe name as the id of the recipe the layer generated for it. */
        String recipe(String id);

        /** The id of the furnace recipe that smelts this 1.12 item string, or null when there is none. */
        String smelting(String inputSpec);
    }

    private PatchouliBooks() {}

    /** Plain mapping (lower case ids only): for books that name no legacy content, and the unit test. */
    public static final Resolver IDENTITY = new Resolver() {
        public String item(String spec) { return spec; }
        public String block(String spec) { return spec; }
        public String entity(String id) { return id; }
        public String recipe(String id) { return id; }
        public String smelting(String inputSpec) { return null; }
    };

    /** The modern path of a staged book file ({@code <book>/<rest>} below {@code patchouli_books/}), or null when the file is not json. */
    public static boolean isJson(String path) {
        return path.endsWith(".json");
    }

    /** {@code book.json}, a category, an entry or a template: decides by the path below the book folder. */
    public static byte[] convert(String relativeToBook, byte[] data, String namespace, Resolver r) {
        JsonElement root;
        try {
            JsonReader reader = new JsonReader(new StringReader(new String(data, StandardCharsets.UTF_8).replace("﻿", "")));
            reader.setLenient(true);
            root = JsonParser.parseReader(reader);
        } catch (RuntimeException e) {
            return data;
        }
        if (!root.isJsonObject()) return data;
        JsonObject obj = root.getAsJsonObject();
        NAMESPACE.set(namespace);
        try {
            if (relativeToBook.equals("book.json")) {
                book(obj, r);
            } else if (relativeToBook.contains("/categories/")) {
                category(obj, r);
            } else if (relativeToBook.contains("/entries/")) {
                entry(obj, namespace, r);
            } else if (relativeToBook.contains("/templates/")) {
                template(obj, namespace, r);
            }
        } finally {
            NAMESPACE.remove();
        }
        return GSON.toJson(obj).getBytes(StandardCharsets.UTF_8);
    }

    /** The namespace of the book being converted: ids without one belong to it. */
    private static final ThreadLocal<String> NAMESPACE = new ThreadLocal<>();

    // book.json -----------------------------------------------------------------------------------------------------------------------

    static void book(JsonObject o, Resolver r) {
        resourceField(o, "book_texture");
        resourceField(o, "filler_texture");
        resourceField(o, "crafting_texture");
        resourceField(o, "open_sound");
        resourceField(o, "flip_sound");
        resourceField(o, "model");
        stackField(o, "custom_book_item", r);
        if (o.has("creative_tab") && o.get("creative_tab").isJsonPrimitive()) {
            String tab = o.get("creative_tab").getAsString().toLowerCase(Locale.ROOT).replace("tab", "").replace(".", "").replace("_", "").trim();
            String mapped = o.get("creative_tab").getAsString().contains(":") ? o.get("creative_tab").getAsString() : CREATIVE_TABS.get(tab);
            if (mapped != null) o.addProperty("creative_tab", mapped);
            else o.remove("creative_tab");
        }
        // 1.12's "advancement_namespaces" and the rest of the fields keep their names
    }

    // categories ----------------------------------------------------------------------------------------------------------------------

    static void category(JsonObject o, Resolver r) {
        iconField(o, "icon", r);
        resourceField(o, "parent");
        resourceField(o, "advancement");
    }

    // entries -------------------------------------------------------------------------------------------------------------------------

    static void entry(JsonObject o, String namespace, Resolver r) {
        iconField(o, "icon", r);
        resourceField(o, "category");
        resourceField(o, "advancement");
        if (o.has("extra_recipe_mappings") && o.get("extra_recipe_mappings").isJsonObject()) {
            JsonObject mapped = new JsonObject();
            for (var e : o.getAsJsonObject("extra_recipe_mappings").entrySet()) mapped.add(r.item(e.getKey()), e.getValue());
            o.add("extra_recipe_mappings", mapped);
        }
        if (o.has("pages") && o.get("pages").isJsonArray()) {
            JsonArray pages = o.getAsJsonArray("pages");
            for (int i = 0; i < pages.size(); i++) {
                JsonElement page = pages.get(i);
                if (page.isJsonObject()) pages.set(i, page(page.getAsJsonObject(), namespace, r));
            }
        }
    }

    /** A page of an entry or a template inclusion; returns the page (a smelting page may become a spotlight page). */
    static JsonObject page(JsonObject p, String namespace, Resolver r) {
        String type = p.has("type") && p.get("type").isJsonPrimitive() ? p.get("type").getAsString() : "";
        String bare = type.startsWith("patchouli:") ? type.substring("patchouli:".length()) : type;
        if (BUILTIN_PAGES.contains(bare)) {
            p.addProperty("type", "patchouli:" + bare);
        } else if (!type.isEmpty()) {
            // a template of the book: referred to by name (with or without the mod prefix)
            p.addProperty("type", type.contains(":") ? Ids1122Lite.resource(type) : namespace + ":" + Ids1122Lite.path(type));
        }
        resourceField(p, "advancement");
        resourceField(p, "anchor", false);
        switch (bare) {
            case "spotlight" -> stackListField(p, "item", r);
            case "crafting" -> {
                recipeField(p, "recipe", r);
                recipeField(p, "recipe2", r);
            }
            case "smelting" -> {
                return smeltingPage(p, r);
            }
            case "entity" -> {
                if (p.has("entity") && p.get("entity").isJsonPrimitive()) p.addProperty("entity", entity(p.get("entity").getAsString(), r));
            }
            case "multiblock" -> multiblock(p, r);
            case "image" -> {
                if (p.has("images") && p.get("images").isJsonArray()) {
                    JsonArray images = p.getAsJsonArray("images");
                    for (int i = 0; i < images.size(); i++) images.set(i, new JsonPrimitive(Ids1122Lite.resource(images.get(i).getAsString())));
                }
            }
            case "relations" -> {
                if (p.has("entries") && p.get("entries").isJsonArray()) {
                    JsonArray entries = p.getAsJsonArray("entries");
                    for (int i = 0; i < entries.size(); i++) entries.set(i, new JsonPrimitive(Ids1122Lite.resource(entries.get(i).getAsString())));
                }
            }
            case "quest" -> resourceField(p, "trigger");
            default -> {
                // template pages: their parameters may name items
                templateValues(p, r);
            }
        }
        return p;
    }

    private static void templateValues(JsonObject p, Resolver r) {
        for (var e : p.entrySet()) {
            String key = e.getKey();
            if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString()) {
                String v = e.getValue().getAsString();
                if (v.startsWith("#") || v.isEmpty()) continue;
                if (key.equals("item") || key.equals("icon") || key.equals("stack")) e.setValue(new JsonPrimitive(joinStacks(v, r)));
            }
        }
    }

    /** A furnace page names the input in 1.12 and the recipe in 1.21: it becomes the recipe page when the layer made one, a spotlight of the input otherwise. */
    private static JsonObject smeltingPage(JsonObject p, Resolver r) {
        JsonObject out = new JsonObject();
        for (var e : p.entrySet()) out.add(e.getKey(), e.getValue());
        boolean ok = true;
        for (String key : new String[] {"recipe", "recipe2"}) {
            if (!p.has(key) || !p.get(key).isJsonPrimitive() || p.get(key).getAsString().isEmpty()) continue;
            String id = r.smelting(p.get(key).getAsString());
            if (id == null) {
                ok = false;
            } else {
                out.addProperty(key, id);
            }
        }
        if (ok) return out;
        JsonObject spot = new JsonObject();
        spot.addProperty("type", "patchouli:spotlight");
        String item = p.has("recipe") ? joinStacks(p.get("recipe").getAsString(), r) : "";
        spot.addProperty("item", item);
        if (p.has("title")) spot.add("title", p.get("title"));
        if (p.has("text")) spot.add("text", p.get("text"));
        if (p.has("anchor")) spot.add("anchor", p.get("anchor"));
        if (p.has("advancement")) spot.add("advancement", p.get("advancement"));
        if (p.has("flag")) spot.add("flag", p.get("flag"));
        if (p.has("op")) spot.add("op", p.get("op"));
        return spot;
    }

    private static void multiblock(JsonObject p, Resolver r) {
        if (p.has("multiblock") && p.get("multiblock").isJsonObject()) {
            JsonObject mb = p.getAsJsonObject("multiblock");
            if (mb.has("mapping") && mb.get("mapping").isJsonObject()) {
                JsonObject mapping = mb.getAsJsonObject("mapping");
                for (var e : mapping.entrySet()) {
                    if (e.getValue().isJsonPrimitive()) e.setValue(new JsonPrimitive(r.block(e.getValue().getAsString())));
                }
            }
        }
        resourceField(p, "multiblock_id");
    }

    // templates -----------------------------------------------------------------------------------------------------------------------

    static void template(JsonObject o, String namespace, Resolver r) {
        if (o.has("components") && o.get("components").isJsonArray()) {
            for (JsonElement c : o.getAsJsonArray("components")) {
                if (!c.isJsonObject()) continue;
                JsonObject comp = c.getAsJsonObject();
                templateValues(comp, r);
                if (comp.has("entity") && comp.get("entity").isJsonPrimitive() && !comp.get("entity").getAsString().startsWith("#")) {
                    comp.addProperty("entity", entity(comp.get("entity").getAsString(), r));
                }
            }
        }
        if (o.has("include") && o.get("include").isJsonArray()) {
            for (JsonElement i : o.getAsJsonArray("include")) {
                if (i.isJsonObject() && i.getAsJsonObject().has("template")) {
                    JsonObject inc = i.getAsJsonObject();
                    String t = inc.get("template").getAsString();
                    inc.addProperty("template", t.contains(":") ? Ids1122Lite.resource(t) : namespace + ":" + Ids1122Lite.path(t));
                }
            }
        }
    }

    // fields --------------------------------------------------------------------------------------------------------------------------

    private static String entity(String spec, Resolver r) {
        if (spec.startsWith("#")) return spec;
        int nbt = spec.indexOf('#');
        return nbt < 0 ? r.entity(spec) : r.entity(spec.substring(0, nbt));
    }

    private static void resourceField(JsonObject o, String key) {
        resourceField(o, key, true);
    }

    private static void resourceField(JsonObject o, String key, boolean namespaced) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive() || !o.get(key).getAsJsonPrimitive().isString()) return;
        String v = o.get(key).getAsString();
        if (v.startsWith("#") || v.isEmpty()) return;
        o.addProperty(key, namespaced ? Ids1122Lite.resource(v) : Ids1122Lite.path(v));
    }

    private static void recipeField(JsonObject o, String key, Resolver r) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) return;
        String v = o.get(key).getAsString();
        if (v.startsWith("#") || v.isEmpty()) return;
        o.addProperty(key, r.recipe(v));
    }

    private static void stackField(JsonObject o, String key, Resolver r) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) return;
        String v = o.get(key).getAsString();
        if (v.startsWith("#") || v.isEmpty()) return;
        o.addProperty(key, r.item(v));
    }

    private static void stackListField(JsonObject o, String key, Resolver r) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) return;
        String v = o.get(key).getAsString();
        if (v.startsWith("#") || v.isEmpty()) return;
        o.addProperty(key, joinStacks(v, r));
    }

    /** "a,b" (an ingredient) with each stack converted; a single stack with NBT braces is kept whole. */
    private static String joinStacks(String v, Resolver r) {
        if (v.indexOf('{') >= 0) return r.item(v);
        StringBuilder sb = new StringBuilder();
        for (String part : v.split(",")) {
            if (sb.length() > 0) sb.append(',');
            String t = part.trim();
            sb.append(t.startsWith("#") || t.startsWith("tag:") ? t : r.item(t));
        }
        return sb.toString();
    }

    /** An icon is a stack or a texture path. */
    private static void iconField(JsonObject o, String key, Resolver r) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) return;
        String v = o.get(key).getAsString();
        if (v.endsWith(".png")) {
            o.addProperty(key, Ids1122Lite.resource(v));
        } else if (!v.startsWith("#") && !v.isEmpty()) {
            o.addProperty(key, joinStacks(v, r));
        }
    }

    /** Resource-id normalization without the registries (the same rules as {@code Ids.normalizePath}). */
    static final class Ids1122Lite {
        private Ids1122Lite() {}

        static String path(String path) {
            return org.kylepitcock.legacyconnector.core.Ids.normalizePath(path);
        }

        static String resource(String id) {
            int c = id.indexOf(':');
            if (c < 0) return NAMESPACE.get() == null ? path(id) : NAMESPACE.get() + ":" + path(id);
            return path(id.substring(0, c)) + ":" + path(id.substring(c + 1));
        }
    }
}
