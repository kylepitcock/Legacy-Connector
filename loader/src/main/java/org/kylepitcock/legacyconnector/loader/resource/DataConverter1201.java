package org.kylepitcock.legacyconnector.loader.resource;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The data and resource side of Forge 1.20.1 to NeoForge 1.21.1: the pack formats changed between the two (1.20.2 to 1.21 renamed the data
 * folders to singular, recipe results and item predicates became item stacks and data components, Forge's {@code forge:} ids are
 * {@code neoforge:}, Forge's tags are the convention tags {@code c:}...). Everything here is a pure function of one file (path and bytes), so a
 * jar translation can run it over every resource entry; the legacy formats it understands are documented per method.
 *
 * <p>What cannot be converted exactly (item NBT other than potions and enchantments, which becomes data components in 1.21) is dropped and
 * counted ({@code nbt_dropped}) rather than left in a form 1.21.1 would reject and discard the whole recipe or table for.
 */
public final class DataConverter1201 {
    private DataConverter1201() {
    }

    /** One output file of a conversion (a conditional recipe can become several). */
    public record Out(String path, byte[] data) {
    }

    /** Counts per category: how many files were changed by which conversion. */
    public static final class Stats {
        public final Map<String, Integer> counts = new TreeMap<>();

        void add(String category) {
            counts.merge(category, 1, Integer::sum);
        }

        public int total() {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }

        @Override
        public String toString() {
            return counts.toString();
        }
    }

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /** Data folders that became singular in 1.21 (1.20.2 to 1.21 renamed them). */
    private static final Map<String, String> PLURAL_DIRS = Map.of(
            "recipes", "recipe", "loot_tables", "loot_table", "advancements", "advancement", "structures", "structure",
            "functions", "function", "item_modifiers", "item_modifier", "predicates", "predicate");
    /** {@code data/<ns>/tags/<type>/}: the registries that were pluralised before 1.21 (newer ones were singular already). */
    private static final Map<String, String> PLURAL_TAGS = Map.of(
            "blocks", "block", "items", "item", "fluids", "fluid", "entity_types", "entity_type", "game_events", "game_event",
            "functions", "function");
    /** Ids that were renamed between 1.20.1 and 1.21.1 and appear in data (JSON data is not run through the data fixers). */
    private static final Map<String, String> RENAMED_IDS = Map.of(
            "minecraft:grass", "minecraft:short_grass", "minecraft:sweeping", "minecraft:sweeping_edge", "minecraft:scute", "minecraft:turtle_scute");
    /** First segment of a Forge tag path to its convention-tag spelling in NeoForge 1.21.1 ({@code c:}); the rest of the path is kept. */
    private static final Map<String, String> TAG_SEGMENTS = Map.ofEntries(
            Map.entry("stone", "stones"), Map.entry("cobblestone", "cobblestones"), Map.entry("sand", "sands"),
            Map.entry("gravel", "gravels"), Map.entry("glass", "glass_blocks"), Map.entry("stained_glass", "glass_blocks"),
            Map.entry("obsidian", "obsidians"), Map.entry("netherrack", "netherracks"), Map.entry("leather", "leathers"),
            Map.entry("string", "strings"), Map.entry("gunpowder", "gunpowders"), Map.entry("concrete", "concretes"),
            Map.entry("glazed_terracotta", "glazed_terracottas"), Map.entry("heads", "skulls"), Map.entry("shears", "tools/shear"));
    private static final Set<String> ITEM_PRED_HOLDERS = Set.of("item", "tool", "weapon", "mainhand", "offhand", "head", "chest", "legs", "feet", "held_item");
    private static final Pattern POTION = Pattern.compile("Potion\\s*:\\s*[\"']([^\"']+)[\"']");
    private static final Pattern ENCHANT_ENTRY = Pattern.compile("\\{\\s*(?:id\\s*:\\s*[\"']([^\"']+)[\"']\\s*,\\s*lvl\\s*:\\s*(\\d+)\\s*[sS]?|lvl\\s*:\\s*(\\d+)\\s*[sS]?\\s*,\\s*id\\s*:\\s*[\"']([^\"']+)[\"'])\\s*\\}");

    // ---- paths -----------------------------------------------------------------------------------------------------------------

    /** The 1.21.1 path of a pack entry: singular data folders, Forge's namespace folders moved to NeoForge's. Other paths are returned as they are. */
    public static String mapPath(String path) {
        if (!path.startsWith("data/")) return path;
        String[] p = path.split("/", 4);
        if (p.length < 4) return path;
        String ns = p[1];
        String dir = p[2];
        String rest = p[3];
        if (ns.equals("forge") && dir.equals("loot_modifiers")) return "data/neoforge/loot_modifiers/" + rest;
        if (ns.equals("forge") && dir.equals("tags")) {
            // data/forge/tags/<type>/<path>.json -> data/c/tags/<type>/<convention path>.json
            int slash = rest.indexOf('/');
            if (slash < 0) return path;
            String type = rest.substring(0, slash);
            type = PLURAL_TAGS.getOrDefault(type, type);
            String tail = rest.substring(slash + 1);
            if (type.equals("worldgen")) return "data/c/tags/worldgen/" + tail;
            return "data/c/tags/" + type + "/" + conventionTagPath(tail);
        }
        if (dir.equals("forge")) return "data/" + ns + "/neoforge/" + rest;
        String plural = PLURAL_DIRS.get(dir);
        if (plural != null) return "data/" + ns + "/" + plural + "/" + rest;
        if (dir.equals("tags")) {
            int slash = rest.indexOf('/');
            if (slash > 0) {
                String type = rest.substring(0, slash);
                String single = PLURAL_TAGS.get(type);
                if (single != null) return "data/" + ns + "/tags/" + single + rest.substring(slash);
            }
        }
        return path;
    }

    private static String pathCategory(String path) {
        String[] p = path.split("/", 4);
        if (p.length >= 3 && (p[2].equals("forge") || p[1].equals("forge"))) return "forge_dir";
        return "plural_dir";
    }

    /** A tag path (without namespace, possibly with a {@code .json} suffix) of the {@code forge:} tags in the {@code c:} spelling. */
    static String conventionTagPath(String path) {
        String suffix = "";
        if (path.endsWith(".json")) {
            suffix = ".json";
            path = path.substring(0, path.length() - 5);
        }
        int slash = path.indexOf('/');
        String first = slash < 0 ? path : path.substring(0, slash);
        if (first.equals("is_coniferous")) first = "is_tree/coniferous"; // biome tags: forge:is_coniferous
        else if (first.equals("is_deciduous")) first = "is_tree/deciduous";
        if (first.contains("/")) return first + (slash < 0 ? "" : path.substring(slash)) + suffix;
        String mapped = TAG_SEGMENTS.get(first);
        if (mapped != null) path = mapped + (slash < 0 ? "" : path.substring(slash));
        return path + suffix;
    }

    /** {@code forge:ingots/iron} or {@code #forge:ingots/iron} to {@code c:...}; anything else unchanged. */
    static String mapTag(String s) {
        boolean hash = s.startsWith("#");
        String id = hash ? s.substring(1) : s;
        if (!id.startsWith("forge:")) return s;
        return (hash ? "#" : "") + "c:" + conventionTagPath(id.substring(6));
    }

    static String mapForgeId(String s) {
        String rest = s.substring(6);
        return "neoforge:" + switch (rest) {
            case "bucket" -> "fluid_container";
            case "item-layers" -> "item_layers";
            case "separate-transforms" -> "separate_transforms";
            case "can_tool_perform_action" -> "can_item_perform_ability";
            default -> rest;
        };
    }

    // ---- entry point ------------------------------------------------------------------------------------------------------------

    /** Converts one resource entry. Entries that are not data/assets JSON come back unchanged (under their mapped path). */
    public static List<Out> convert(String path, byte[] data, Stats stats) {
        String np = mapPath(path);
        boolean dataJson = path.startsWith("data/") && path.endsWith(".json");
        boolean model = path.startsWith("assets/") && path.endsWith(".json") && path.contains("/models/");
        if (!dataJson && !model) {
            if (!np.equals(path)) stats.add(pathCategory(path));
            return List.of(new Out(np, data));
        }
        Ctx c = new Ctx();
        if (!np.equals(path)) c.hit(pathCategory(path));
        JsonElement root;
        try {
            String text = new String(data, StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            JsonReader reader = new JsonReader(new StringReader(text));
            reader.setLenient(true);
            root = JsonParser.parseReader(reader);
        } catch (RuntimeException e) {
            stats.add("unparsable");
            if (!np.equals(path)) stats.add(pathCategory(path));
            return List.of(new Out(np, data));
        }
        if (root == null || !root.isJsonObject() && !root.isJsonArray()) {
            c.flush(stats);
            return List.of(new Out(np, data));
        }
        List<Out> outs = new ArrayList<>();
        String[] parts = np.split("/", 4);
        String dir = parts.length > 2 ? parts[2] : "";
        if (model) {
            generic(root, c, true);
            outs.add(new Out(np, bytes(root)));
        } else if (dir.equals("recipe") && root.isJsonObject()) {
            outs.addAll(recipe(np, root.getAsJsonObject(), c));
        } else if (dir.equals("advancement") && root.isJsonObject()) {
            outs.addAll(advancement(np, root.getAsJsonObject(), c));
        } else {
            if (dir.equals("loot_table") || dir.equals("loot_modifiers")) loot(root, c);
            if (dir.equals("dimension_type") && root.isJsonObject()) dimensionType(root.getAsJsonObject(), c);
            if (np.contains("/worldgen/structure_set/") && root.isJsonObject()) structureSet(root.getAsJsonObject(), c);
            generic(root, c, false);
            outs.add(new Out(np, bytes(root)));
        }
        c.flush(stats);
        return outs;
    }

    /** Converts a whole set of entries (a jar's resources); {@code pack.mcmeta} is brought to the 1.21.1 formats, using what the set contains. */
    public static Map<String, byte[]> convertAll(Map<String, byte[]> entries, Stats stats) {
        boolean hasAssets = false;
        boolean hasData = false;
        for (String n : entries.keySet()) {
            if (n.startsWith("assets/")) hasAssets = true;
            else if (n.startsWith("data/")) hasData = true;
        }
        Map<String, byte[]> out = new LinkedHashMap<>();
        for (var e : entries.entrySet()) {
            String name = e.getKey();
            if (name.equals("pack.mcmeta")) {
                byte[] converted = packMcmeta(new String(e.getValue(), StandardCharsets.UTF_8), hasAssets, hasData);
                if (!java.util.Arrays.equals(converted, e.getValue())) stats.add("pack_format");
                out.put(name, converted);
                continue;
            }
            if (name.startsWith("data/") || name.startsWith("assets/")) {
                for (Out o : convert(name, e.getValue(), stats)) {
                    byte[] previous = out.get(o.path());
                    out.put(o.path(), previous != null && o.path().contains("/tags/") ? mergeTags(previous, o.data()) : o.data());
                }
            } else {
                out.put(name, e.getValue());
            }
        }
        return out;
    }

    /** Two tag files that landed on one path (data/forge/tags/items/glass.json and data/forge/tags/items/glass_blocks.json both become c:glass_blocks): their values add up. */
    private static byte[] mergeTags(byte[] a, byte[] b) {
        try {
            JsonObject x = JsonParser.parseString(new String(a, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject y = JsonParser.parseString(new String(b, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray values = x.has("values") && x.get("values").isJsonArray() ? x.getAsJsonArray("values") : new JsonArray();
            if (y.has("values") && y.get("values").isJsonArray()) values.addAll(y.getAsJsonArray("values"));
            x.add("values", values);
            return bytes(x);
        } catch (RuntimeException e) {
            return b;
        }
    }

    /**
     * {@code pack.mcmeta}: 1.21.1 reads format 34 for resources and 48 for data. A mod pack that has both declares the data format and the whole
     * range as supported, which is what NeoForge's own pack does; the Forge-only {@code forge:*_pack_format} keys go.
     */
    public static byte[] packMcmeta(String json, boolean hasAssets, boolean hasData) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return json.getBytes(StandardCharsets.UTF_8);
            JsonObject o = root.getAsJsonObject();
            JsonObject pack = o.has("pack") && o.get("pack").isJsonObject() ? o.getAsJsonObject("pack") : new JsonObject();
            pack.addProperty("pack_format", hasData ? 48 : 34);
            JsonArray range = new JsonArray();
            range.add(0);
            range.add(Integer.MAX_VALUE);
            pack.add("supported_formats", range);
            pack.remove("forge:resource_pack_format");
            pack.remove("forge:data_pack_format");
            o.remove("forge:resource_pack_format");
            o.remove("forge:data_pack_format");
            o.add("pack", pack);
            return GSON.toJson(o).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            // lenient JSON with comments: a plain replacement of the number
            return json.replaceAll("(\"pack_format\"\\s*:\\s*)\\d+", "$1" + (hasData ? 48 : 34)).getBytes(StandardCharsets.UTF_8);
        }
    }

    // ---- the file-level state ---------------------------------------------------------------------------------------------------

    private static final class Ctx {
        final Set<String> categories = new LinkedHashSet<>();

        void hit(String category) {
            categories.add(category);
        }

        void flush(Stats stats) {
            for (String c : categories) stats.add(c);
        }
    }

    private static byte[] bytes(JsonElement e) {
        return GSON.toJson(e).getBytes(StandardCharsets.UTF_8);
    }

    // ---- generic pass: ids, tags, renamed ids -------------------------------------------------------------------------------------

    /** Walks any JSON: {@code forge:} type/loader/condition ids, {@code forge:} tags, renamed ids, {@code forge_data}, model parents. */
    private static void generic(JsonElement e, Ctx c, boolean model) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            for (String k : new ArrayList<>(o.keySet())) {
                JsonElement v = o.get(k);
                if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                    String s = v.getAsString();
                    String r = s;
                    if ((k.equals("type") || k.equals("loader") || k.equals("condition")) && s.startsWith("forge:")) {
                        r = mapForgeId(s);
                        c.hit(model && k.equals("loader") ? "model_loader" : "forge_ids");
                    } else if (k.equals("parent") && model && s.startsWith("forge:")) {
                        r = "neoforge:" + s.substring(6);
                        c.hit("model_loader");
                    } else if (k.equals("tag") && (s.startsWith("forge:") || s.startsWith("#forge:"))) {
                        r = mapTag(s);
                        c.hit("forge_tags");
                    } else if (!model) {
                        r = mapValue(s, c);
                    }
                    if (!r.equals(s)) o.addProperty(k, r);
                } else {
                    generic(v, c, model);
                }
            }
            if (o.has("forge_data")) {
                JsonElement v = o.remove("forge_data");
                o.add("neoforge_data", v);
                c.hit("model_loader");
            }
        } else if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (int i = 0; i < a.size(); i++) {
                JsonElement v = a.get(i);
                if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                    if (!model) {
                        String s = v.getAsString();
                        String r = mapValue(s, c);
                        if (!r.equals(s)) a.set(i, new JsonPrimitive(r));
                    }
                } else {
                    generic(v, c, model);
                }
            }
        }
    }

    private static String mapValue(String s, Ctx c) {
        if (s.startsWith("#forge:")) {
            c.hit("forge_tags");
            return mapTag(s);
        }
        String renamed = RENAMED_IDS.get(s);
        if (renamed != null) {
            c.hit("renamed_ids");
            return renamed;
        }
        return s;
    }

    // ---- conditions ---------------------------------------------------------------------------------------------------------------

    /** A top-level {@code conditions} (Forge's) is {@code neoforge:conditions} in NeoForge; the condition types are renamed by the generic pass. */
    private static void conditionsKey(JsonObject o, Ctx c) {
        if (o.has("conditions") && o.get("conditions").isJsonArray() && !o.has("neoforge:conditions")) {
            JsonElement v = o.remove("conditions");
            o.add("neoforge:conditions", v);
            c.hit("recipe_conditions");
        }
    }

    // ---- recipes ------------------------------------------------------------------------------------------------------------------

    private static List<Out> recipe(String path, JsonObject o, Ctx c) {
        List<Out> outs = new ArrayList<>();
        String type = o.has("type") && o.get("type").isJsonPrimitive() ? o.get("type").getAsString() : "";
        if ((type.equals("forge:conditional") || type.equals("neoforge:conditional")) && o.has("recipes") && o.get("recipes").isJsonArray()) {
            // 1.21 has no conditional recipe: every branch is a recipe of its own with the branch's conditions
            c.hit("recipe_conditional");
            JsonArray branches = o.getAsJsonArray("recipes");
            int i = 0;
            for (JsonElement b : branches) {
                if (!b.isJsonObject() || !b.getAsJsonObject().has("recipe") || !b.getAsJsonObject().get("recipe").isJsonObject()) continue;
                JsonObject branch = b.getAsJsonObject();
                JsonObject r = branch.getAsJsonObject("recipe");
                if (branch.has("conditions")) {
                    r.remove("conditions");
                    r.add("conditions", branch.get("conditions"));
                }
                Ctx bc = new Ctx();
                recipeBody(r, bc);
                generic(r, bc, false);
                c.categories.addAll(bc.categories);
                String p = i == 0 ? path : path.substring(0, path.length() - 5) + "_alt" + i + ".json";
                outs.add(new Out(p, bytes(r)));
                i++;
            }
            if (!outs.isEmpty()) return outs;
        }
        recipeBody(o, c);
        generic(o, c, false);
        outs.add(new Out(path, bytes(o)));
        return outs;
    }

    private static void recipeBody(JsonObject o, Ctx c) {
        conditionsKey(o, c);
        String type = o.has("type") && o.get("type").isJsonPrimitive() ? o.get("type").getAsString() : "";
        boolean vanilla = !type.contains(":") || type.startsWith("minecraft:");
        if (!vanilla) return;
        String t = type.startsWith("minecraft:") ? type.substring(10) : type;
        boolean hasResult = o.has("result");
        if (hasResult) {
            JsonElement res = o.get("result");
            if (res.isJsonPrimitive() || (res.isJsonObject() && !res.getAsJsonObject().has("id"))) {
                JsonObject stack = stack(res, c, "recipe_result");
                if (t.equals("stonecutting") && o.has("count") && !stack.has("count")) stack.add("count", o.remove("count"));
                o.add("result", stack);
            }
        }
        for (String k : new String[] {"ingredient", "ingredients", "template", "base", "addition"}) {
            if (o.has(k)) o.add(k, ingredient(o.get(k), c));
        }
        if (o.has("key") && o.get("key").isJsonObject()) {
            JsonObject key = o.getAsJsonObject("key");
            for (String k : new ArrayList<>(key.keySet())) key.add(k, ingredient(key.get(k), c));
        }
    }

    /** {@code "minecraft:stone"} or {@code {item, count, nbt}} to {@code {id, count, components}}. */
    private static JsonObject stack(JsonElement e, Ctx c, String category) {
        JsonObject out = new JsonObject();
        if (e.isJsonPrimitive()) {
            out.addProperty("id", e.getAsString());
            c.hit(category);
            return out;
        }
        JsonObject o = e.getAsJsonObject();
        c.hit(category);
        for (var en : new ArrayList<>(o.entrySet())) {
            switch (en.getKey()) {
                case "item" -> out.add("id", en.getValue());
                case "nbt" -> {
                    JsonObject comps = components(en.getValue(), c);
                    if (comps.size() > 0) out.add("components", comps);
                    c.hit("recipe_nbt");
                }
                default -> out.add(en.getKey(), en.getValue());
            }
        }
        return out;
    }

    private static JsonElement ingredient(JsonElement e, Ctx c) {
        if (e.isJsonArray()) {
            JsonArray a = new JsonArray();
            for (JsonElement x : e.getAsJsonArray()) a.add(ingredient(x, c));
            return a;
        }
        if (!e.isJsonObject()) return e;
        JsonObject o = e.getAsJsonObject();
        String type = o.has("type") && o.get("type").isJsonPrimitive() ? o.get("type").getAsString() : "";
        if (type.equals("forge:nbt") || type.equals("forge:partial_nbt") || type.equals("forge:strict_nbt")) {
            JsonObject comps = o.has("nbt") ? components(o.get("nbt"), c) : new JsonObject();
            c.hit("recipe_nbt");
            JsonElement item = o.has("item") ? o.get("item") : o.get("items");
            if (item == null) item = JsonNull();
            JsonObject r = new JsonObject();
            if (comps.size() > 0 && item.isJsonPrimitive()) {
                r.addProperty("type", "neoforge:components");
                r.add("items", item);
                r.add("components", comps);
                r.addProperty("strict", type.equals("forge:strict_nbt"));
            } else if (item.isJsonPrimitive()) {
                r.add("item", item);
            } else {
                r = o;
            }
            return r;
        }
        if (o.has("nbt") && (o.has("item") || o.has("id"))) {
            o.remove("nbt");
            c.hit("recipe_nbt");
        }
        for (String k : new String[] {"base", "subtracted"}) if (o.has(k)) o.add(k, ingredient(o.get(k), c));
        if (o.has("children")) o.add("children", ingredient(o.get("children"), c));
        return o;
    }

    private static JsonElement JsonNull() {
        return com.google.gson.JsonNull.INSTANCE;
    }

    /** The parts of a legacy item NBT that have a data component form here: potions and enchantments; everything else is dropped. */
    private static JsonObject components(JsonElement nbt, Ctx c) {
        JsonObject comps = new JsonObject();
        String s = nbt.isJsonPrimitive() ? nbt.getAsString() : nbt.toString();
        Matcher m = POTION.matcher(s);
        if (m.find()) {
            JsonObject pc = new JsonObject();
            pc.addProperty("potion", m.group(1));
            comps.add("minecraft:potion_contents", pc);
        }
        int ench = Math.max(s.indexOf("StoredEnchantments"), -1);
        boolean stored = ench >= 0;
        int at = stored ? ench : s.indexOf("Enchantments");
        if (at >= 0) {
            JsonObject levels = new JsonObject();
            Matcher em = ENCHANT_ENTRY.matcher(s.substring(at));
            while (em.find()) {
                String id = em.group(1) != null ? em.group(1) : em.group(4);
                String lvl = em.group(2) != null ? em.group(2) : em.group(3);
                levels.addProperty(id, Integer.parseInt(lvl));
            }
            if (levels.size() > 0) {
                JsonObject holder = new JsonObject();
                holder.add("levels", levels);
                comps.add(stored ? "minecraft:stored_enchantments" : "minecraft:enchantments", holder);
            }
        }
        if (comps.size() == 0) c.hit("nbt_dropped");
        return comps;
    }

    // ---- advancements --------------------------------------------------------------------------------------------------------------

    private static List<Out> advancement(String path, JsonObject o, Ctx c) {
        List<Out> outs = new ArrayList<>();
        if (!o.has("criteria") && o.has("advancements") && o.get("advancements").isJsonArray()) {
            // Forge's conditional advancement: the first branch
            c.hit("recipe_conditional");
            int i = 0;
            for (JsonElement b : o.getAsJsonArray("advancements")) {
                if (!b.isJsonObject() || !b.getAsJsonObject().has("advancement") || !b.getAsJsonObject().get("advancement").isJsonObject()) continue;
                JsonObject branch = b.getAsJsonObject();
                JsonObject adv = branch.getAsJsonObject("advancement");
                if (branch.has("conditions")) adv.add("conditions", branch.get("conditions"));
                Ctx bc = new Ctx();
                advancementBody(adv, bc);
                generic(adv, bc, false);
                c.categories.addAll(bc.categories);
                outs.add(new Out(i == 0 ? path : path.substring(0, path.length() - 5) + "_alt" + i + ".json", bytes(adv)));
                i++;
            }
            if (!outs.isEmpty()) return outs;
        }
        advancementBody(o, c);
        generic(o, c, false);
        outs.add(new Out(path, bytes(o)));
        return outs;
    }

    private static void advancementBody(JsonObject o, Ctx c) {
        conditionsKey(o, c);
        if (o.has("display") && o.get("display").isJsonObject()) {
            JsonObject d = o.getAsJsonObject("display");
            if (d.has("icon") && d.get("icon").isJsonObject()) {
                JsonObject icon = d.getAsJsonObject("icon");
                if (icon.has("item") || icon.has("nbt")) {
                    d.add("icon", stack(icon, c, "adv_icon"));
                }
            }
        }
        itemPredicates(o, c);
    }

    // ---- item predicates -----------------------------------------------------------------------------------------------------------

    /** Finds item predicates by the keys they appear under (trigger conditions, entity equipment) and converts them. */
    private static void itemPredicates(JsonElement e, Ctx c) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            for (String k : new ArrayList<>(o.keySet())) {
                JsonElement v = o.get(k);
                if (ITEM_PRED_HOLDERS.contains(k) && v.isJsonObject()) itemPredicate(v.getAsJsonObject(), c);
                else if (k.equals("items") && v.isJsonArray()) {
                    for (JsonElement x : v.getAsJsonArray()) if (x.isJsonObject()) itemPredicate(x.getAsJsonObject(), c);
                }
                itemPredicates(v, c);
            }
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) itemPredicates(x, c);
        }
    }

    /** 1.20.1 {@code {items, tag, nbt, enchantments, potion, durability}} to 1.21: {@code items} holder set and {@code predicates} (sub-predicates by component). */
    private static void itemPredicate(JsonObject o, Ctx c) {
        boolean changed = false;
        JsonObject preds = o.has("predicates") && o.get("predicates").isJsonObject() ? o.getAsJsonObject("predicates") : new JsonObject();
        if (o.has("tag") && o.get("tag").isJsonPrimitive()) {
            String tag = o.remove("tag").getAsString();
            if (tag.startsWith("forge:")) tag = mapTag(tag);
            if (!o.has("items")) o.addProperty("items", "#" + tag);
            changed = true;
        }
        if (o.has("nbt")) {
            preds.add("minecraft:custom_data", o.remove("nbt"));
            changed = true;
        }
        if (o.has("enchantments") && o.get("enchantments").isJsonArray()) {
            preds.add("minecraft:enchantments", enchantList(o.remove("enchantments").getAsJsonArray()));
            changed = true;
        }
        if (o.has("stored_enchantments") && o.get("stored_enchantments").isJsonArray()) {
            preds.add("minecraft:stored_enchantments", enchantList(o.remove("stored_enchantments").getAsJsonArray()));
            changed = true;
        }
        if (o.has("potion")) {
            preds.add("minecraft:potion_contents", o.remove("potion"));
            changed = true;
        }
        if (o.has("durability")) {
            JsonObject dmg = new JsonObject();
            dmg.add("durability", o.remove("durability"));
            preds.add("minecraft:damage", dmg);
            changed = true;
        }
        if (changed) {
            if (preds.size() > 0) o.add("predicates", preds);
            c.hit("item_predicate");
        }
    }

    private static JsonArray enchantList(JsonArray old) {
        JsonArray out = new JsonArray();
        for (JsonElement x : old) {
            if (!x.isJsonObject()) continue;
            JsonObject e = x.getAsJsonObject();
            JsonObject n = new JsonObject();
            if (e.has("enchantment")) n.add("enchantments", e.get("enchantment"));
            if (e.has("levels")) n.add("levels", e.get("levels"));
            out.add(n);
        }
        return out;
    }

    // ---- loot tables and loot modifiers ------------------------------------------------------------------------------------------

    private static String id(String s) {
        return s.contains(":") ? s : "minecraft:" + s;
    }

    private static void loot(JsonElement e, Ctx c) {
        if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) loot(x, c);
            return;
        }
        if (!e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        if (o.has("function") && o.get("function").isJsonPrimitive()) lootFunction(o, c);
        if (o.has("condition") && o.get("condition").isJsonPrimitive()) lootCondition(o, c);
        if (o.has("type") && o.get("type").isJsonPrimitive() && id(o.get("type").getAsString()).equals("minecraft:loot_table") && o.has("name")) {
            o.add("value", o.remove("name"));
            c.hit("loot_function");
        }
        for (var en : new ArrayList<>(o.entrySet())) loot(en.getValue(), c);
        itemPredicatesOfLoot(o, c);
    }

    private static void itemPredicatesOfLoot(JsonObject o, Ctx c) {
        if (o.has("condition") && o.get("condition").isJsonPrimitive() && id(o.get("condition").getAsString()).equals("minecraft:entity_properties") && o.has("predicate")) {
            itemPredicates(o.get("predicate"), c);
        }
    }

    private static void lootFunction(JsonObject o, Ctx c) {
        String f = id(o.get("function").getAsString());
        switch (f) {
            case "minecraft:set_nbt" -> {
                String tag = o.has("tag") && o.get("tag").isJsonPrimitive() ? o.get("tag").getAsString() : "";
                Matcher m = POTION.matcher(tag);
                if (m.find() && tag.replaceAll("\\s", "").matches("\\{Potion:[\"'][^\"']+[\"']\\}")) {
                    o.addProperty("function", "minecraft:set_potion");
                    o.remove("tag");
                    o.addProperty("id", m.group(1));
                } else {
                    o.addProperty("function", "minecraft:set_custom_data");
                }
                c.hit("loot_function");
            }
            case "minecraft:copy_nbt" -> {
                o.addProperty("function", "minecraft:copy_custom_data");
                c.hit("loot_function");
            }
            case "minecraft:looting_enchant" -> {
                o.addProperty("function", "minecraft:enchanted_count_increase");
                if (!o.has("enchantment")) o.addProperty("enchantment", "minecraft:looting");
                c.hit("loot_function");
            }
            case "minecraft:set_attributes" -> {
                if (o.has("modifiers") && o.get("modifiers").isJsonArray()) {
                    for (JsonElement m : o.getAsJsonArray("modifiers")) {
                        if (!m.isJsonObject()) continue;
                        JsonObject mod = m.getAsJsonObject();
                        mod.remove("uuid");
                        if (!mod.has("id")) {
                            String name = mod.has("name") && mod.get("name").isJsonPrimitive() ? mod.get("name").getAsString() : "modifier";
                            mod.addProperty("id", "legacy:" + name.toLowerCase().replaceAll("[^a-z0-9_./-]", "_"));
                        }
                        if (mod.has("operation") && mod.get("operation").isJsonPrimitive()) {
                            String op = mod.get("operation").getAsString();
                            mod.addProperty("operation", switch (op) {
                                case "addition" -> "add_value";
                                case "multiply_base" -> "add_multiplied_base";
                                case "multiply_total" -> "add_multiplied_total";
                                default -> op;
                            });
                        }
                    }
                    c.hit("loot_function");
                }
            }
            case "minecraft:enchant_with_levels" -> {
                if (o.has("treasure")) {
                    boolean treasure = o.remove("treasure").getAsBoolean();
                    if (!treasure && !o.has("options")) o.addProperty("options", "#minecraft:non_treasure");
                    c.hit("loot_function");
                }
            }
            case "minecraft:enchant_randomly" -> {
                if (o.has("enchantments") && !o.has("options")) {
                    o.add("options", o.remove("enchantments"));
                    c.hit("loot_function");
                }
            }
            case "minecraft:set_contents" -> {
                if (o.has("type") && !o.has("component")) {
                    o.remove("type");
                    o.addProperty("component", "minecraft:container");
                    c.hit("loot_function");
                }
            }
            default -> {
            }
        }
    }

    private static void lootCondition(JsonObject o, Ctx c) {
        String cond = id(o.get("condition").getAsString());
        switch (cond) {
            case "minecraft:random_chance_with_looting" -> {
                double chance = o.has("chance") ? o.remove("chance").getAsDouble() : 0;
                double mult = o.has("looting_multiplier") ? o.remove("looting_multiplier").getAsDouble() : 0;
                o.addProperty("condition", "minecraft:random_chance_with_enchanted_bonus");
                o.addProperty("unenchanted_chance", chance);
                JsonObject linear = new JsonObject();
                linear.addProperty("type", "minecraft:linear");
                linear.addProperty("base", chance + mult);
                linear.addProperty("per_level_above_first", mult);
                o.add("enchanted_chance", linear);
                o.addProperty("enchantment", "minecraft:looting");
                c.hit("loot_condition");
            }
            case "forge:can_tool_perform_action" -> {
                o.addProperty("condition", "neoforge:can_item_perform_ability");
                if (o.has("action")) o.add("ability", o.remove("action"));
                c.hit("forge_ids");
            }
            case "minecraft:match_tool" -> {
                if (o.has("predicate") && o.get("predicate").isJsonObject()) itemPredicate(o.getAsJsonObject("predicate"), c);
            }
            default -> {
            }
        }
    }

    // ---- worldgen ------------------------------------------------------------------------------------------------------------------

    /**
     * 1.21 fails the whole world load when one worldgen element does not parse, where 1.20.1 only logged it: a structure set's exclusion zone
     * allows 1 to 16 chunks and some packs say more (Dungeons and Taverns says 17).
     */
    private static void structureSet(JsonObject o, Ctx c) {
        if (!o.has("placement") || !o.get("placement").isJsonObject()) return;
        JsonObject placement = o.getAsJsonObject("placement");
        if (!placement.has("exclusion_zone") || !placement.get("exclusion_zone").isJsonObject()) return;
        JsonObject zone = placement.getAsJsonObject("exclusion_zone");
        // other_set is one structure set id: a list (which 1.20.1 skipped with a log line) is read as its first entry
        if (zone.has("other_set") && zone.get("other_set").isJsonArray()) {
            JsonArray sets = zone.getAsJsonArray("other_set");
            if (sets.size() > 0) zone.add("other_set", sets.get(0));
            else zone.remove("other_set");
            c.hit("worldgen_range");
        }
        if (zone.has("chunk_count") && zone.get("chunk_count").isJsonPrimitive() && zone.get("chunk_count").getAsInt() > 16) {
            zone.addProperty("chunk_count", 16);
            c.hit("worldgen_range");
        }
    }

    // ---- dimension types -----------------------------------------------------------------------------------------------------------

    /** 1.21 requires the monster spawn light fields of a dimension type. */
    private static void dimensionType(JsonObject o, Ctx c) {
        if (!o.has("monster_spawn_light_level")) {
            JsonObject light = new JsonObject();
            light.addProperty("type", "minecraft:uniform");
            light.addProperty("min_inclusive", 0);
            light.addProperty("max_inclusive", 7);
            o.add("monster_spawn_light_level", light);
            c.hit("dimension_type");
        }
        if (!o.has("monster_spawn_block_light_limit")) {
            o.addProperty("monster_spawn_block_light_limit", 0);
            c.hit("dimension_type");
        }
    }
}
