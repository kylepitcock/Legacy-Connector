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
 * <p>Item NBT is migrated structurally ({@link ItemComponents1201}, {@link Snbt1201}): the keys Minecraft owns become data components, the rest goes
 * to {@code minecraft:custom_data}. What cannot be converted exactly (unparsable NBT, a key only some items read on an item we cannot name) is dropped
 * and counted ({@code nbt_dropped}) rather than left in a form 1.21.1 would reject and discard the whole recipe or table for.
 *
 * <p>Portions adapted from Retromod (https://github.com/Bownlux/Retromod), MIT License, Copyright (c) 2026 Bownlux: the loot entry item scope of
 * {@code set_nbt}, the killer to attacker entity target renames and the item predicate rules ({@code resources/ItemStackDataMigrator.java}); the
 * convention tag pairs are read from NeoForge's own tag definitions ({@code tools/dev/gen_c_tag_pairs.py}), with Retromod's table as a cross-check only.
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
    /** Attribute ids whose identifiers changed after Forge 1.20.1 / Minecraft 1.20.1. */
    private static final Map<String, String> ATTRIBUTE_IDS = Map.ofEntries(
            Map.entry("forge:step_height_addition", "minecraft:generic.step_height"),
            Map.entry("forge:entity_gravity", "minecraft:generic.gravity"),
            Map.entry("forge:swim_speed", "neoforge:swim_speed"),
            Map.entry("generic.movement_speed", "minecraft:generic.movement_speed"),
            // 1.16 renamed the camel case names (mods kept writing them: Forge 1.20.1 read them through the data fixer, 1.21.1 does not); 1.20.5 renamed the horse one
            Map.entry("generic.maxHealth", "minecraft:generic.max_health"), Map.entry("generic.followRange", "minecraft:generic.follow_range"),
            Map.entry("generic.knockbackResistance", "minecraft:generic.knockback_resistance"), Map.entry("generic.movementSpeed", "minecraft:generic.movement_speed"),
            Map.entry("generic.flyingSpeed", "minecraft:generic.flying_speed"), Map.entry("generic.attackDamage", "minecraft:generic.attack_damage"),
            Map.entry("generic.attackKnockback", "minecraft:generic.attack_knockback"), Map.entry("generic.attackSpeed", "minecraft:generic.attack_speed"),
            Map.entry("generic.armorToughness", "minecraft:generic.armor_toughness"), Map.entry("horse.jumpStrength", "minecraft:generic.jump_strength"),
            Map.entry("horse.jump_strength", "minecraft:generic.jump_strength"), Map.entry("minecraft:horse.jump_strength", "minecraft:generic.jump_strength"),
            Map.entry("zombie.spawnReinforcements", "minecraft:zombie.spawn_reinforcements"));
    /** First segment of a Forge tag path to its convention-tag spelling in NeoForge 1.21.1 ({@code c:}); the rest of the path is kept. */
    private static final Map<String, String> TAG_SEGMENTS = Map.ofEntries(
            Map.entry("stone", "stones"), Map.entry("cobblestone", "cobblestones"), Map.entry("sand", "sands"),
            Map.entry("gravel", "gravels"), Map.entry("glass", "glass_blocks"), Map.entry("stained_glass", "glass_blocks"),
            Map.entry("obsidian", "obsidians"), Map.entry("netherrack", "netherracks"), Map.entry("leather", "leathers"),
            Map.entry("string", "strings"), Map.entry("gunpowder", "gunpowders"), Map.entry("concrete", "concretes"),
            Map.entry("glazed_terracotta", "glazed_terracottas"), Map.entry("heads", "skulls"), Map.entry("shears", "tools/shear"));
    /** Tags whose whole path is spelled differently in NeoForge's 1.21 convention tags (tool types went singular). */
    private static final Map<String, String> TAG_PATHS = Map.of(
            "tools/shields", "tools/shield", "tools/bows", "tools/bow", "tools/crossbows", "tools/crossbow",
            "tools/fishing_rods", "tools/fishing_rod", "tools/tridents", "tools/spear", "tools/shears", "tools/shear");
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
            if (type.equals("worldgen")) {
                if (tail.startsWith("biome/")) return "data/c/tags/worldgen/biome/" + tagPairs().getOrDefault(tail.substring(6, tail.length() - (tail.endsWith(".json") ? 5 : 0)), tail.substring(6, tail.length() - (tail.endsWith(".json") ? 5 : 0))) + (tail.endsWith(".json") ? ".json" : "");
                return "data/c/tags/worldgen/" + tail;
            }
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

    private static volatile Map<String, String> tagPairs;

    /**
     * {@code forge-c-tag-pairs.tsv}: the Forge tag paths NeoForge's own convention tags replace under another name, read from the {@code #forge:}
     * entries of its {@code data/c/tags} by {@code tools/dev/gen_c_tag_pairs.py}. Wins over the segment rules below.
     */
    private static Map<String, String> tagPairs() {
        Map<String, String> r = tagPairs;
        if (r != null) return r;
        r = new java.util.HashMap<>();
        try (var in = DataConverter1201.class.getResourceAsStream("/legacyconnector/forward/1.20.1/forge-c-tag-pairs.tsv")) {
            if (in != null) {
                for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] a = line.strip().split("\t");
                    if (a.length == 2) r.put(a[0], a[1]);
                }
            }
        } catch (java.io.IOException e) {
            // no table: the segment rules alone
        }
        tagPairs = r;
        return r;
    }

    /** A tag path (without namespace, possibly with a {@code .json} suffix) of the {@code forge:} tags in the {@code c:} spelling. */
    static String conventionTagPath(String path) {
        String suffix = "";
        if (path.endsWith(".json")) {
            suffix = ".json";
            path = path.substring(0, path.length() - 5);
        }
        String paired = tagPairs().get(path);
        if (paired != null) return paired + suffix;
        int slash = path.indexOf('/');
        String first = slash < 0 ? path : path.substring(0, slash);
        if (first.equals("is_coniferous")) first = "is_tree/coniferous"; // biome tags: forge:is_coniferous
        else if (first.equals("is_deciduous")) first = "is_tree/deciduous";
        if (first.contains("/")) return first + (slash < 0 ? "" : path.substring(slash)) + suffix;
        String whole = TAG_PATHS.get(path);
        if (whole != null) return whole + suffix;
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

    /** Attribute id migration shared by JSON data and binary structure entity NBT. */
    static String mapAttributeId(String id) {
        return ATTRIBUTE_IDS.getOrDefault(id, id);
    }

    // ---- entry point ------------------------------------------------------------------------------------------------------------

    /** Converts one resource entry. Apart from binary structure NBT, entries other than data/assets JSON keep their bytes and mapped path. */
    public static List<Out> convert(String path, byte[] data, Stats stats) {
        String np = mapPath(path);
        boolean dataJson = path.startsWith("data/") && path.endsWith(".json");
        boolean model = path.startsWith("assets/") && path.endsWith(".json") && path.contains("/models/");
        if (!dataJson && !model) {
            if (!np.equals(path)) stats.add(pathCategory(path));
            if (path.startsWith("data/") && path.endsWith(".nbt") && np.contains("/structure/")) {
                StructureNbtConverter1201.Result converted = StructureNbtConverter1201.convert(data);
                if (converted.changed()) stats.add("structure_nbt_attribute_ids");
                if (converted.malformed()) stats.add("structure_nbt_unparsable");
                return List.of(new Out(np, converted.data()));
            }
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
            if (dir.equals("item_modifier")) {
                // Forge 1.20.1 accepted a single item-modifier object; 1.21.1 dispatches a list of functions.
                if (root.isJsonObject()) {
                    JsonArray functions = new JsonArray();
                    functions.add(root);
                    root = functions;
                    c.hit("item_modifier_array");
                }
                itemModifier(root, c);
            }
            if (np.contains("/palladium/powers/") && root.isJsonObject()) {
                palladiumPower(root.getAsJsonObject(), c);
            }
            if (dir.equals("loot_table") || dir.equals("loot_modifiers") || dir.equals("predicate")) loot(root, c);
            if (dir.equals("dimension_type") && root.isJsonObject()) dimensionType(root.getAsJsonObject(), c);
            if (np.contains("/worldgen/structure_set/") && root.isJsonObject()) structureSet(root.getAsJsonObject(), c);
            if (np.contains("/worldgen/") || dir.equals("dimension_type")) worldgenShapes(root, c);
            // a tag file with "replace": "false" (a string; 1.20.1 read it leniently) is a boolean in 1.21
            if (np.contains("/tags/") && root.isJsonObject() && root.getAsJsonObject().has("replace") && root.getAsJsonObject().get("replace").isJsonPrimitive()
                    && root.getAsJsonObject().get("replace").getAsJsonPrimitive().isString()) {
                root.getAsJsonObject().addProperty("replace", Boolean.parseBoolean(root.getAsJsonObject().get("replace").getAsString()));
                c.hit("forge_tags");
            }
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
        emptyTagsForUnknownReferences(out, stats);
        return out;
    }

    private static volatile Set<String> neoForgeTags;
    private static final Pattern TAG_KEY_REF = Pattern.compile("\"tag\"\\s*:\\s*\"#?c:([a-z0-9_./-]+)\"");
    private static final Pattern HASH_REF = Pattern.compile("\"#c:([a-z0-9_./-]+)\"");

    /** The {@code c:} tags NeoForge 1.21.1 ships ({@code <type>/<path>}), listed in {@code c-tags.txt}. */
    private static Set<String> neoForgeTags() {
        Set<String> r = neoForgeTags;
        if (r != null) return r;
        r = new java.util.HashSet<>();
        try (var in = DataConverter1201.class.getResourceAsStream("/legacyconnector/forward/1.20.1/c-tags.txt")) {
            if (in != null) for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) if (!line.isBlank()) r.add(line.trim());
        } catch (java.io.IOException e) {
            // no list: every referenced tag gets an (empty) file, which merges with the real one
        }
        neoForgeTags = r;
        return r;
    }

    /**
     * A recipe, loot table or advancement that names a tag nobody defines does not load (the recipe fails to parse). Every {@code c:} tag the converted data
     * references that neither NeoForge nor this pack defines gets an empty tag file; a pack that does define it adds its values on top (tags merge).
     */
    private static void emptyTagsForUnknownReferences(Map<String, byte[]> out, Stats stats) {
        Map<String, Set<String>> wanted = new TreeMap<>();
        Set<String> defined = new java.util.HashSet<>();
        for (var e : out.entrySet()) {
            String n = e.getKey();
            if (!n.startsWith("data/") || !n.endsWith(".json")) continue;
            String[] p = n.split("/", 5);
            if (p.length < 5) continue;
            if (p[1].equals("c") && p[2].equals("tags")) {
                String rest = n.substring("data/c/tags/".length());
                defined.add(rest.substring(0, rest.length() - 5));
            }
            String kind = p[2];
            String[] types;
            if (kind.equals("recipe")) types = new String[] {"item"};
            else if (kind.equals("loot_table") || kind.equals("advancement")) types = new String[] {"item", "block"};
            else if (kind.equals("tags") && !p[3].equals("worldgen")) types = new String[] {p[3]};
            else continue;
            String text = new String(e.getValue(), StandardCharsets.UTF_8);
            if (text.indexOf("c:") < 0) continue;
            for (Pattern pat : new Pattern[] {TAG_KEY_REF, HASH_REF}) {
                Matcher m = pat.matcher(text);
                while (m.find()) {
                    boolean entryOfTag = kind.equals("tags");
                    // a "tag" key in a tag file is not a reference of that spelling; only "#c:" strings are
                    if (entryOfTag && pat == TAG_KEY_REF) continue;
                    for (String t : types) wanted.computeIfAbsent(t, k -> new java.util.TreeSet<>()).add(m.group(1));
                }
            }
        }
        Set<String> known = neoForgeTags();
        int added = 0;
        for (var e : wanted.entrySet()) {
            for (String path : e.getValue()) {
                String key = e.getKey() + "/" + path;
                if (known.contains(key) || defined.contains(key)) continue;
                out.put("data/c/tags/" + key + ".json", "{\"values\":[]}".getBytes(StandardCharsets.UTF_8));
                added++;
            }
        }
        for (int i = 0; i < added; i++) stats.add("empty_tags");
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
        /** The item of the loot entry being walked (its functions say whose NBT they set), or null when it is not one item. */
        String entryItem;

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
            neoforgeConditions(o, c);
            for (String k : new ArrayList<>(o.keySet())) {
                JsonElement v = o.get(k);
                if (k.toLowerCase(java.util.Locale.ROOT).contains("command")) {
                    o.add(k, commandValues(v, c));
                    continue;
                }
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
                    } else if (k.equals("attribute") && !mapAttributeId(s).equals(s)) {
                        r = mapAttributeId(s);
                        c.hit("attribute_ids");
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

    /**
     * A single component inside an array is semantically identical to the
     * component object itself. Normalize this legacy form in Palladium's
     * name_change abilities so the 1.21.1 component codec receives the same
     * value without constructing an unnecessary array-backed sibling chain.
     */
    private static void palladiumPower(JsonObject power, Ctx c) {
        if (!power.has("abilities") || !power.get("abilities").isJsonObject()) return;
        JsonObject abilities = power.getAsJsonObject("abilities");
        for (Map.Entry<String, JsonElement> entry : new ArrayList<>(abilities.entrySet())) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject ability = entry.getValue().getAsJsonObject();
            if (!ability.has("type") || !ability.get("type").isJsonPrimitive()
                    || !ability.get("type").getAsString().equals("palladium:name_change")) continue;
            for (String property : List.of("name", "name_cached")) {
                JsonElement value = ability.get(property);
                if (value != null && value.isJsonArray() && value.getAsJsonArray().size() == 1) {
                    ability.add(property, value.getAsJsonArray().get(0));
                    c.hit("palladium_component_arrays");
                }
            }
        }
    }


    /**
     * Forge worldgen resources use a literal "forge:conditions" object key.
     * NeoForge reads the same condition list from "neoforge:conditions".
     * When both forms exist, retain every condition by appending the legacy
     * entries to the modern array; preserve malformed/conflicting shapes.
     */
    private static void neoforgeConditions(JsonObject o, Ctx c) {
        JsonElement legacy = o.remove("forge:conditions");
        if (legacy == null) return;

        JsonElement modern = o.get("neoforge:conditions");
        if (modern == null) {
            o.add("neoforge:conditions", legacy);
            c.hit("forge_conditions");
        } else if (legacy.isJsonArray() && modern.isJsonArray()) {
            JsonArray merged = new JsonArray();
            merged.addAll(modern.getAsJsonArray());
            merged.addAll(legacy.getAsJsonArray());
            o.add("neoforge:conditions", merged);
            c.hit("forge_conditions_merged");
        } else {
            // Do not discard either representation when their shapes conflict.
            o.add("forge:conditions", legacy);
            c.hit("forge_conditions_conflict");
        }
    }

    /** Converts legacy function identifiers and fields inside item-modifier JSON resources. */
    private static void itemModifier(JsonElement e, Ctx c) {
        if (e.isJsonArray()) {
            splitSetNbt(e.getAsJsonArray(), c);
            for (JsonElement child : e.getAsJsonArray()) itemModifier(child, c);
            return;
        }
        if (!e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        if (o.has("function") && o.get("function").isJsonPrimitive()) {
            String function = o.get("function").getAsString();
            renameEntityTarget(o, ENTITY_TARGET_FUNCTIONS.get(id(function)), c);
            if (function.equals("minecraft:set_nbt")) {
                o.addProperty("function", "minecraft:set_custom_data");
                c.hit("item_modifier");
            } else if (function.equals("minecraft:set_lore")) {
                JsonElement replace = o.remove("replace");
                if (!o.has("mode")) {
                    boolean replaceAll = replace != null && replace.isJsonPrimitive()
                            && replace.getAsJsonPrimitive().isBoolean() && replace.getAsBoolean();
                    o.addProperty("mode", replaceAll ? "replace_all" : "append");
                }
                c.hit("item_modifier");
            }
        }
        for (Map.Entry<String, JsonElement> child : new ArrayList<>(o.entrySet())) {
            itemModifier(child.getValue(), c);
        }
    }

    /** Converts command strings embedded in Palladium power JSON with the same dialect migration as .mcfunction files. */
    private static JsonElement commandValues(JsonElement value, Ctx c) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String source = value.getAsString();
            String migrated = FunctionConverter1201.convertCommand(source);
            if (!migrated.equals(source)) c.hit("command_payloads");
            return new JsonPrimitive(migrated);
        }
        if (value.isJsonArray()) {
            JsonArray converted = new JsonArray();
            for (JsonElement child : value.getAsJsonArray()) converted.add(commandValues(child, c));
            return converted;
        }
        if (value.isJsonObject()) {
            JsonObject converted = new JsonObject();
            for (Map.Entry<String, JsonElement> child : value.getAsJsonObject().entrySet()) {
                converted.add(child.getKey(), commandValues(child.getValue(), c));
            }
            return converted;
        }
        return value;
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
                    JsonObject comps = components(en.getValue(), c, o.has("item") && o.get("item").isJsonPrimitive() ? o.get("item").getAsString() : null, true);
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
            JsonObject comps = o.has("nbt") ? components(o.get("nbt"), c, o.has("item") && o.get("item").isJsonPrimitive() ? o.get("item").getAsString() : o.has("items") && o.get("items").isJsonPrimitive() ? o.get("items").getAsString() : null, type.equals("forge:strict_nbt")) : new JsonObject();
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

    /**
     * A legacy item NBT as the 1.21.1 data components of a stack of {@code itemId} (null when not known): the keys Minecraft owns become components
     * ({@link ItemComponents1201}), the mod's own keys and the Minecraft keys that cannot be carried over go to {@code minecraft:custom_data} when
     * {@code keepCustomData} (a partial NBT ingredient matches by the components it names, so the custom data of the NBT stays out of it).
     */
    private static JsonObject components(JsonElement nbt, Ctx c, String itemId, boolean keepCustomData) {
        Snbt1201.Compound tag = Snbt1201.parseField(nbt);
        ItemComponents1201.Split split = tag == null ? null : ItemComponents1201.split(itemId, tag);
        if (split == null) {
            // unparsable NBT, or a key that only some of Minecraft's items read on an item we cannot name: the NBT does not carry over
            c.hit("nbt_dropped");
            return new JsonObject();
        }
        JsonObject comps = split.components();
        if (comps.size() > 0) c.hit("nbt_components");
        if (keepCustomData && !split.customData().entries().isEmpty()) {
            comps.addProperty("minecraft:custom_data", Snbt1201.write(split.customData()));
            c.hit("nbt_custom_data");
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
        if (o.has("criteria")) advancementLocationPredicates(o.get("criteria"), c);
    }

    /**
     * Minecraft 1.21.1's LocationPredicate codec calls the structure holder set
     * "structures". Some legacy mod advancements use the singular "structure"
     * key; leaving that unknown key behind silently drops the location filter
     * and can award every structure-discovery advancement immediately.
     */
    private static void advancementLocationPredicates(JsonElement element, Ctx c) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) advancementLocationPredicates(child, c);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : new ArrayList<>(object.entrySet())) {
            JsonElement value = entry.getValue();
            if ("location".equals(entry.getKey()) && value.isJsonObject()) {
                JsonObject location = value.getAsJsonObject();
                if (location.has("structure") && !location.has("structures")
                        && location.get("structure").isJsonPrimitive()
                        && location.get("structure").getAsJsonPrimitive().isString()) {
                    location.add("structures", location.remove("structure"));
                    c.hit("advancement_location_structures");
                }
            }
            advancementLocationPredicates(value, c);
        }
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
            JsonElement nbt = o.remove("nbt");
            if (!nbtPredicate(nbt, predicateItem(o), preds)) preds.add("minecraft:custom_data", nbt);
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

    /** The one item a legacy item predicate names, or null (a tag, a list, none). */
    private static String predicateItem(JsonObject o) {
        JsonElement items = o.has("items") ? o.get("items") : o.get("item");
        if (items == null) return null;
        if (items.isJsonPrimitive() && !items.getAsString().startsWith("#")) return id(items.getAsString());
        if (items.isJsonArray() && items.getAsJsonArray().size() == 1 && items.getAsJsonArray().get(0).isJsonPrimitive()) return id(items.getAsJsonArray().get(0).getAsString());
        return null;
    }

    /**
     * A legacy predicate NBT whose Minecraft keys now are components, which {@code custom_data} never matches: the potion, the enchantments and the damage
     * have a sub-predicate that tests what the NBT test tested. Anything else of Minecraft's (a name, a model number) has none, and the NBT stays a
     * custom data test as before. The mod's own keys stay a custom data test.
     *
     * @return false when nothing was converted (the caller keeps the NBT as custom data)
     */
    private static boolean nbtPredicate(JsonElement nbt, String item, JsonObject preds) {
        Snbt1201.Compound tag = Snbt1201.parseField(nbt);
        if (tag == null) return false;
        ItemComponents1201.Split split = ItemComponents1201.split(item, tag);
        if (split == null || !split.unmapped().isEmpty() || split.components().size() == 0) return false;
        JsonObject staged = new JsonObject();
        for (var en : split.components().entrySet()) {
            JsonElement v = en.getValue();
            switch (en.getKey()) {
                case "minecraft:potion_contents" -> {
                    if (!v.isJsonObject() || v.getAsJsonObject().size() != 1 || !v.getAsJsonObject().has("potion")) return false;
                    staged.add(en.getKey(), v.getAsJsonObject().get("potion"));
                }
                case "minecraft:enchantments", "minecraft:stored_enchantments" -> {
                    JsonArray list = new JsonArray();
                    for (var lv : v.getAsJsonObject().getAsJsonObject("levels").entrySet()) {
                        JsonObject exact = new JsonObject();
                        exact.add("min", lv.getValue());
                        exact.add("max", lv.getValue());
                        JsonObject entry = new JsonObject();
                        entry.addProperty("enchantments", lv.getKey());
                        entry.add("levels", exact);
                        list.add(entry);
                    }
                    staged.add(en.getKey(), list);
                }
                case "minecraft:damage" -> {
                    JsonObject dmg = new JsonObject();
                    dmg.add("damage", v);
                    staged.add(en.getKey(), dmg);
                }
                default -> {
                    return false;
                }
            }
        }
        for (var en : staged.entrySet()) if (preds.has(en.getKey())) return false;
        staged.entrySet().forEach(en -> preds.add(en.getKey(), en.getValue()));
        if (!split.customData().entries().isEmpty()) preds.addProperty("minecraft:custom_data", Snbt1201.write(split.customData()));
        return true;
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
            splitSetNbt(e.getAsJsonArray(), c);
            for (JsonElement x : e.getAsJsonArray()) loot(x, c);
            return;
        }
        if (!e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        String enclosingItem = c.entryItem;
        c.entryItem = scopeItem(o, enclosingItem);
        try {
            lootObject(o, c);
        } finally {
            c.entryItem = enclosingItem;
        }
    }

    private static void lootObject(JsonObject o, Ctx c) {
        // block state predicates: 1.20.1 accepted a boolean or a number for a property value, 1.21 wants the string ("ropelogged": false)
        if (o.has("properties") && o.get("properties").isJsonObject() && o.has("block")) {
            JsonObject props = o.getAsJsonObject("properties");
            for (String key : new ArrayList<>(props.keySet())) {
                JsonElement v = props.get(key);
                if (v.isJsonPrimitive() && !v.getAsJsonPrimitive().isString()) {
                    props.addProperty(key, v.getAsString());
                    c.hit("loot_function");
                }
            }
        }
        if (o.has("function") && o.get("function").isJsonPrimitive()) {
            renameEntityTarget(o, ENTITY_TARGET_FUNCTIONS.get(id(o.get("function").getAsString())), c);
            lootFunction(o, c);
        }
        if (o.has("condition") && o.get("condition").isJsonPrimitive()) {
            String condition = id(o.get("condition").getAsString());
            if (condition.equals("minecraft:entity_properties") || condition.equals("minecraft:entity_scores")) renameEntityTarget(o, "entity", c);
            lootCondition(o, c);
        }
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

    /** Loot functions that name an entity target, by the field that holds it. */
    private static final Map<String, String> ENTITY_TARGET_FUNCTIONS = Map.of(
            "minecraft:copy_name", "source", "minecraft:copy_nbt", "source", "minecraft:set_name", "entity",
            "minecraft:set_lore", "entity", "minecraft:fill_player_head", "entity");
    /** The loot context targets 1.21 renamed (read from LootContext$EntityTarget): the old names are rejected, not ignored. */
    private static final Map<String, String> RENAMED_ENTITY_TARGETS = Map.of(
            "killer", "attacker", "direct_killer", "direct_attacker", "killer_player", "attacking_player");

    private static void renameEntityTarget(JsonObject o, String field, Ctx c) {
        if (field == null || !o.has(field) || !o.get(field).isJsonPrimitive()) return;
        String renamed = RENAMED_ENTITY_TARGETS.get(o.get(field).getAsString());
        if (renamed == null) return;
        o.addProperty(field, renamed);
        c.hit("loot_entity_target");
    }

    /** The one item a loot entry, composite entry, pool or table can produce, or null (several items, a tag or another table). */
    private static String scopeItem(JsonObject o, String enclosing) {
        String type = o.has("type") && o.get("type").isJsonPrimitive() ? id(o.get("type").getAsString()) : null;
        if ("minecraft:item".equals(type) && o.has("name") && o.get("name").isJsonPrimitive()) return id(o.get("name").getAsString());
        if (o.has("entries")) return commonItem(o.get("entries"));
        if (o.has("children")) return commonItem(o.get("children"));
        if (o.has("pools") && o.get("pools").isJsonArray()) {
            String common = null;
            for (JsonElement pool : o.getAsJsonArray("pools")) {
                String item = pool.isJsonObject() ? commonItem(pool.getAsJsonObject().get("entries")) : null;
                if (item == null || common != null && !common.equals(item)) return null;
                common = item;
            }
            return common;
        }
        return enclosing;
    }

    private static String commonItem(JsonElement entries) {
        if (entries == null || !entries.isJsonArray()) return null;
        String common = null;
        for (JsonElement el : entries.getAsJsonArray()) {
            if (!el.isJsonObject()) return null;
            JsonObject entry = el.getAsJsonObject();
            String type = entry.has("type") && entry.get("type").isJsonPrimitive() ? id(entry.get("type").getAsString()) : null;
            if ("minecraft:empty".equals(type)) continue;
            String item = "minecraft:item".equals(type) && entry.has("name") && entry.get("name").isJsonPrimitive() ? id(entry.get("name").getAsString())
                    : Set.of("minecraft:alternatives", "minecraft:group", "minecraft:sequence").contains(type) ? commonItem(entry.get("children")) : null;
            if (item == null || common != null && !common.equals(item)) return null;
            common = item;
        }
        return common;
    }

    /**
     * Replaces each {@code set_nbt} of a function list by the functions that set what its NBT means in 1.21.1: {@code set_components} for the keys
     * Minecraft owns and {@code set_custom_data} (which merges, as {@code set_nbt} did) for the mod's own keys and what could not be carried over. A
     * {@code set_nbt} that cannot be split (a key only some items read, on an item we cannot name) is left for {@link #lootFunction}.
     */
    private static void splitSetNbt(JsonArray functions, Ctx c) {
        for (int i = 0; i < functions.size(); i++) {
            JsonElement el = functions.get(i);
            if (!el.isJsonObject()) continue;
            JsonObject f = el.getAsJsonObject();
            if (f.has("type") || !f.has("function") || !f.get("function").isJsonPrimitive() || !id(f.get("function").getAsString()).equals("minecraft:set_nbt")) continue;
            Snbt1201.Compound tag = f.has("tag") ? Snbt1201.parseField(f.get("tag")) : null;
            if (tag == null) continue;
            // a pure potion keeps its short form (set_potion), as before
            if (tag.entries().size() == 1 && tag.string("Potion") != null) continue;
            ItemComponents1201.Split split = ItemComponents1201.split(c.entryItem, tag);
            if (split == null || split.components().size() == 0) continue; // nothing of Minecraft's: lootFunction turns it into set_custom_data
            JsonObject rest = f.deepCopy();
            rest.remove("function");
            rest.remove("tag");
            List<JsonObject> out = new ArrayList<>();
            JsonObject components = split.components();
            boolean custom = !split.customData().entries().isEmpty();
            if (custom && (rest.has("conditions") || rest.has("condition"))) {
                // two functions would each roll the conditions on their own: the custom data joins the components (set_nbt merged, this replaces)
                components.addProperty("minecraft:custom_data", Snbt1201.write(split.customData()));
                custom = false;
            }
            out.add(withFields("minecraft:set_components", "components", components, rest));
            if (custom) out.add(withFields("minecraft:set_custom_data", "tag", new JsonPrimitive(Snbt1201.write(split.customData())), rest));
            functions.remove(i);
            for (int k = 0; k < out.size(); k++) functions.asList().add(i + k, out.get(k));
            i += out.size() - 1;
            c.hit("loot_set_components");
        }
    }

    private static JsonObject withFields(String function, String key, JsonElement value, JsonObject rest) {
        JsonObject out = new JsonObject();
        out.addProperty("function", function);
        out.add(key, value);
        for (var en : rest.entrySet()) out.add(en.getKey(), en.getValue().deepCopy());
        return out;
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
        // a tag where one structure set is expected (1.20.1 skipped the set with a log line): the exclusion zone cannot be expressed, so it goes
        if (zone.has("other_set") && zone.get("other_set").isJsonPrimitive() && zone.get("other_set").getAsString().startsWith("#")) {
            placement.remove("exclusion_zone");
            c.hit("worldgen_range");
            return;
        }
        if (zone.has("chunk_count") && zone.get("chunk_count").isJsonPrimitive() && zone.get("chunk_count").getAsInt() > 16) {
            zone.addProperty("chunk_count", 16);
            c.hit("worldgen_range");
        }
    }

    // ---- int providers and spawner categories ------------------------------------------------------------------------------------------

    private static final Set<String> NESTED_INT_PROVIDERS = Set.of("minecraft:uniform", "minecraft:biased_to_bottom", "minecraft:clamped", "minecraft:clamped_normal");
    private static final Set<String> VANILLA_MOB_CATEGORIES = Set.of("monster", "creature", "ambient", "axolotls", "underground_water_creature", "water_creature", "water_ambient", "misc");

    /**
     * 1.20.1 wrote an int provider other than a constant as {@code {"type": "minecraft:uniform", "value": {"min_inclusive": 1, "max_inclusive": 3}}} (the
     * dispatch codec nested the fields of a plain codec); 1.20.2 and later put the fields next to the type. Height providers were always flat and have no
     * {@code value} object, so a {@code uniform} object with one is an int provider. Also: a biome's {@code spawners} of a mod's own mob category (Forge
     * enum extension, gone here) are read as the monster category.
     */
    private static void worldgenShapes(JsonElement e, Ctx c) {
        if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) worldgenShapes(x, c);
            return;
        }
        if (!e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        if (o.has("type") && o.get("type").isJsonPrimitive() && NESTED_INT_PROVIDERS.contains(o.get("type").getAsString()) && o.has("value") && o.get("value").isJsonObject()) {
            JsonObject inner = o.getAsJsonObject("value");
            o.remove("value");
            for (Map.Entry<String, JsonElement> en : inner.entrySet()) o.add(en.getKey(), en.getValue());
            c.hit("int_provider");
        }
        if (o.has("spawners") && o.get("spawners").isJsonObject() && !o.has("type")) {
            JsonObject spawners = o.getAsJsonObject("spawners");
            for (String key : new ArrayList<>(spawners.keySet())) {
                if (VANILLA_MOB_CATEGORIES.contains(key) || !spawners.get(key).isJsonArray()) continue;
                JsonElement list = spawners.remove(key);
                JsonElement existing = spawners.get("monster");
                if (existing != null && existing.isJsonArray() && list.isJsonArray()) existing.getAsJsonArray().addAll(list.getAsJsonArray());
                else spawners.add("monster", list);
                c.hit("mob_category");
            }
        }
        for (Map.Entry<String, JsonElement> en : new ArrayList<>(o.entrySet())) worldgenShapes(en.getValue(), c);
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
