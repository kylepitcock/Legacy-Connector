package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Mechanical, registry-independent asset fixes applied while a legacy jar is rewritten.
 *
 * <p>Anything that needs to know what a block or item <em>is</em> (blockstates for metadata, recipes with
 * damage values, ore dictionary ingredients) is translated later at runtime by the shim's generated
 * resource/data pack. Here we only fix things that would stop resources from loading at all:</p>
 * <ul>
 *     <li>Modern {@code ResourceLocation}s reject upper case and most punctuation, which 1.7.10 allowed in
 *     both namespaces and paths ({@code assets/IC2/textures/blocks/CopperOre.png}).</li>
 *     <li>{@code .lang} files became {@code .json} in 1.13.</li>
 *     <li>The block atlas only stitches {@code textures/block/}; legacy mods use {@code textures/blocks/}.</li>
 *     <li>1.12 JSON recipes moved from {@code assets/} to {@code data/} and changed format; they are staged
 *     under {@code legacyconnector_legacy/} for the runtime translator.</li>
 * </ul>
 */
public final class LegacyResourceConverter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Legacy texture folders that must be added to the modern atlases. */
    private static final String BLOCK_ATLAS = """
            {
              "sources": [
                { "type": "directory", "source": "blocks", "prefix": "blocks/" },
                { "type": "directory", "source": "items", "prefix": "items/" }
              ]
            }
            """;

    private final Map<String, byte[]> output = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> langs = new LinkedHashMap<>();
    private boolean sawTextures;
    private final CtmBridge ctm = new CtmBridge();

    /**
     * @return true if the entry was consumed (written to the output, possibly renamed, or dropped on purpose).
     */
    public boolean accept(String path, byte[] data) {
        if (path.equals("pack.mcmeta") || path.equals("pack.png")) {
            return true; // NeoForge synthesizes mod pack metadata; a pack_format 1 file only confuses it.
        }
        if (!path.startsWith("assets/")) return false;

        String modern = normalizePath(path);
        if (modern.contains("/textures/")) sawTextures = true;

        if (modern.endsWith(".lang")) {
            // assets/<ns>/lang/en_us.lang -> merged into assets/<ns>/lang/en_us.json
            String jsonPath = modern.substring(0, modern.length() - ".lang".length()) + ".json";
            Map<String, String> entries = langs.computeIfAbsent(jsonPath, k -> new LinkedHashMap<>());
            parseLang(new String(data, StandardCharsets.UTF_8), entries);
            return true;
        }
        if (modern.matches("assets/[^/]+/recipes/.+\\.json")) {
            output.putIfAbsent("legacyconnector_legacy/" + modern, data);
            return true;
        }
        if (modern.matches("assets/[^/]+/patchouli_books/.+")) {
            // 1.12 Patchouli books live in assets; the modern Patchouli reads data/<ns>/patchouli_books. The runtime converts the staged
            // copy (item and block ids need the registries) when the modern Patchouli is installed; the original stays for a legacy Patchouli.
            output.putIfAbsent("legacyconnector_legacy/" + modern, data);
            // the modern Patchouli finds a mod's books by scanning its jar for data/<mod>/patchouli_books/<book>/book.json
            String bookJson = PatchouliBookJson.dataPath(modern);
            if (bookJson != null) output.putIfAbsent(bookJson, PatchouliBookJson.convert(data));
        }
        if (modern.matches("assets/[^/]+/sounds\\.json")) {
            data = convertSounds(data, modern.substring("assets/".length(), modern.indexOf('/', "assets/".length())));
        } else if (modern.endsWith(".mcmeta")) {
            data = sanitizeMcmeta(data);
        } else if (modern.endsWith(".json") && (modern.contains("/models/") || modern.contains("/blockstates/"))) {
            data = lowercaseResourceRefs(data);
            if (modern.contains("/models/")) addVineModels(data);
        }
        ctm.offer(modern, data);
        output.putIfAbsent(modern, data);
        return true;
    }

    /** A model that names one of the 1.12 vanilla vine models as parent gets it generated into the minecraft namespace (1.13 has no such file). */
    private void addVineModels(byte[] data) {
        java.util.regex.Matcher m = VINE_MODEL.matcher(new String(data, StandardCharsets.UTF_8));
        while (m.find()) {
            String json = vineModel(m.group(1));
            if (json != null) output.putIfAbsent("assets/minecraft/models/block/" + m.group(1) + ".json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Called once all entries are seen; returns every resource to write into the transformed jar. */
    public Map<String, byte[]> finish() {
        for (var lang : langs.entrySet()) {
            // A real .json lang shipped next to the .lang wins.
            if (output.containsKey(lang.getKey())) continue;
            JsonObject obj = new JsonObject();
            lang.getValue().forEach(obj::addProperty);
            output.put(lang.getKey(), GSON.toJson(obj).getBytes(StandardCharsets.UTF_8));
        }
        ctm.apply(output);
        if (sawTextures) {
            output.putIfAbsent("assets/minecraft/atlases/blocks.json", BLOCK_ATLAS.getBytes(StandardCharsets.UTF_8));
        }
        return output;
    }

    /**
     * Lower-cases and sanitizes a resource path. Shared convention with the runtime, which applies the same
     * function to every ResourceLocation a legacy mod constructs.
     */
    public static String normalizePath(String path) {
        StringBuilder sb = new StringBuilder(path.length());
        for (char c : path.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-' ? c : '_');
        }
        return sb.toString();
    }

    /**
     * 1.7.10 read .mcmeta leniently; mods shipped trailing commas and comments, which the strict 1.21 reader rejects (and
     * then drops the whole sprite). Parse leniently, drop the null an extra comma leaves in an array, write strict JSON.
     */
    static byte[] sanitizeMcmeta(byte[] data) {
        try {
            String text = new String(data, StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            JsonElement root = JsonParser.parseString(text);
            dropNullElements(root);
            return GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return data;
        }
    }

    private static void dropNullElements(JsonElement e) {
        if (e.isJsonArray()) {
            JsonArray array = e.getAsJsonArray();
            for (int i = array.size() - 1; i >= 0; i--) {
                if (array.get(i).isJsonNull()) array.remove(i);
                else dropNullElements(array.get(i));
            }
        } else if (e.isJsonObject()) {
            for (var entry : e.getAsJsonObject().entrySet()) dropNullElements(entry.getValue());
        }
    }

    static void parseLang(String text, Map<String, String> out) {
        boolean escapes = false;
        for (String line : text.split("\\r?\\n")) {
            if (line.startsWith("﻿")) line = line.substring(1);
            if (line.replace(" ", "").equals("#PARSE_ESCAPES")) { // "#PARSE_ESCAPES" and "# PARSE_ESCAPES" both occur
                escapes = true;
                continue;
            }
            if (line.isBlank() || line.startsWith("#") || line.startsWith("//")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String value = line.substring(eq + 1);
            String key = line.substring(0, eq);
            if (escapes) {
                // Properties-style escapes; keys carry them too (Electroblob's Wizardry: "tile.ebwizardry\:arcane_workbench.name")
                value = unescapeLang(value);
                key = unescapeLang(key);
            }
            // 1.7.10 used Java format specifiers verbatim (%s, %d); modern Component formatting accepts %s and
            // positional %1$s only. Numeric conversions are rewritten so translations do not render raw.
            value = value.replaceAll("%(\\d+\\$)?[dfx]", "%$1s").replaceAll("%(\\d+\\$)?\\.\\d+f", "%$1s");
            out.put(key, value);
        }
    }

    /** java.util.Properties escapes: \n \t \r and unicode escapes, and a backslash before any other character (\: \= \\ ...) stands for that character. */
    static String unescapeLang(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                b.append(c);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'r' -> b.append('\r');
                case 'u' -> {
                    if (i + 4 < s.length() && s.substring(i + 1, i + 5).matches("[0-9a-fA-F]{4}")) {
                        b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                        i += 4;
                    } else {
                        b.append("\\u");
                    }
                }
                default -> b.append(n);
            }
        }
        return b.toString();
    }

    /**
     * 1.7.10 sounds.json: sound paths without a namespace meant the mod's own domain (1.21 assumes minecraft), names
     * could contain upper case, and "category" is gone (the volume slider is chosen when a sound is played): it is kept as
     * "legacy_category", which the modern parser ignores and the sound bridge reads to pick the slider. Event keys are
     * sanitized like every other resource path.
     */
    static byte[] convertSounds(byte[] data, String namespace) {
        try {
            JsonObject root = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject out = new JsonObject();
            for (var e : root.entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonObject event = e.getValue().getAsJsonObject().deepCopy();
                JsonElement category = event.remove("category");
                if (category != null) event.add("legacy_category", category);
                if (event.has("sounds") && event.get("sounds").isJsonArray()) {
                    JsonArray sounds = new JsonArray();
                    for (JsonElement snd : event.getAsJsonArray("sounds")) {
                        if (snd.isJsonPrimitive()) {
                            sounds.add(soundRef(snd.getAsString(), namespace));
                        } else if (snd.isJsonObject()) {
                            JsonObject o = snd.getAsJsonObject().deepCopy();
                            if (o.has("name")) o.addProperty("name", soundRef(o.get("name").getAsString(), namespace));
                            sounds.add(o);
                        }
                    }
                    event.add("sounds", sounds);
                }
                out.add(normalizePath(e.getKey()), event);
            }
            return GSON.toJson(out).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return data;
        }
    }

    private static String soundRef(String ref, String namespace) {
        int colon = ref.indexOf(':');
        return colon < 0 ? namespace + ":" + normalizePath(ref) : normalizeRef(ref);
    }

    /** Model/blockstate JSON written for 1.7.10-1.12 may reference textures and parents with upper case. */
    static byte[] lowercaseResourceRefs(byte[] data) {
        String text = new String(data, StandardCharsets.UTF_8);
        try {
            JsonElement root = JsonParser.parseString(text);
            fixRefs(root, null);
            return GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            // a file cut off before its closing brackets (BiblioCraft's models/item/atlascover.json) is closed here
            String closed = closeTruncated(text);
            if (closed != null) {
                try {
                    JsonElement root = JsonParser.parseString(closed);
                    fixRefs(root, null);
                    return GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
                } catch (RuntimeException again) {
                    // not just a missing tail
                }
            }
            return data; // keep malformed JSON byte-identical; the vanilla loader will report it
        }
    }

    /** The JSON text with the brackets still open at its end closed, or null when it is not cut off inside brackets. */
    static String closeTruncated(String text) {
        StringBuilder open = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
            } else if (c == '"') {
                inString = true;
            } else if (c == '{' || c == '[') {
                open.append(c);
            } else if (c == '}' || c == ']') {
                if (open.length() == 0) return null;
                open.setLength(open.length() - 1);
            }
        }
        if (inString || open.length() == 0) return null;
        StringBuilder out = new StringBuilder(text.stripTrailing());
        if (out.length() > 0 && out.charAt(out.length() - 1) == ',') out.setLength(out.length() - 1);
        for (int i = open.length() - 1; i >= 0; i--) out.append(open.charAt(i) == '{' ? '}' : ']');
        return out.toString();
    }

    private static void fixRefs(JsonElement element, String key) {
        if (element instanceof JsonObject obj) {
            // item override predicates name properties ("bodyModel"): modern ids take lower case only (the runtime registers them lower-cased)
            if ("overrides".equals(key) || obj.has("predicate")) {
                var predicate = obj.get("predicate");
                if (predicate instanceof JsonObject p) {
                    JsonObject fixed = new JsonObject();
                    for (var pe : p.entrySet()) fixed.add(normalizeRef(pe.getKey()), pe.getValue());
                    obj.add("predicate", fixed);
                }
            }
            for (var e : obj.entrySet()) {
                if (e.getValue() instanceof JsonPrimitive p && p.isString() && isRefKey(e.getKey(), key)) {
                    String fixed = normalizeRef(p.getAsString());
                    if ("textures".equals(key)) fixed = vanillaTexture(fixed);
                    else if (e.getKey().equals("parent")) fixed = vanillaModel(fixed);
                    e.setValue(new JsonPrimitive(fixed));
                } else {
                    fixRefs(e.getValue(), e.getKey());
                }
            }
        } else if (element instanceof JsonArray arr) {
            for (JsonElement e : arr) fixRefs(e, key);
        }
    }

    private static boolean isRefKey(String key, String parentKey) {
        return key.equals("parent") || key.equals("model") || "textures".equals(parentKey);
    }

    /**
     * Vanilla models renamed after 1.12 (parents of mod models). The texture slots of the new models are the ones of the old (wall, texture, pane,
     * edge, top, bottom, torch, cross), so a rename is enough; the vine_* models no longer exist and are generated ({@link #vineModel}).
     */
    private static final Map<String, String> MODEL_RENAMES = Map.ofEntries(
            Map.entry("block/half_slab", "block/slab"),
            Map.entry("block/upper_slab", "block/slab_top"),
            Map.entry("block/grass_path", "block/dirt_path"),
            Map.entry("block/door_bottom", "block/door_bottom_left"),
            Map.entry("block/door_top", "block/door_top_left"),
            Map.entry("block/door_bottom_rh", "block/door_bottom_right"),
            Map.entry("block/door_top_rh", "block/door_top_right"),
            Map.entry("block/slime", "block/slime_block"),
            Map.entry("block/grass", "block/grass_block"),
            Map.entry("block/wall_post", "block/template_wall_post"),
            Map.entry("block/wall_side", "block/template_wall_side"),
            Map.entry("block/fence_gate_closed", "block/template_fence_gate"),
            Map.entry("block/fence_gate_open", "block/template_fence_gate_open"),
            Map.entry("block/wall_gate_closed", "block/template_fence_gate_wall"),
            Map.entry("block/wall_gate_open", "block/template_fence_gate_wall_open"),
            Map.entry("block/pane_post", "block/template_glass_pane_post"),
            Map.entry("block/pane_side", "block/template_glass_pane_side"),
            Map.entry("block/pane_side_alt", "block/template_glass_pane_side_alt"),
            Map.entry("block/pane_noside", "block/template_glass_pane_noside"),
            Map.entry("block/pane_noside_alt", "block/template_glass_pane_noside_alt"),
            Map.entry("block/wooden_trapdoor_top", "block/template_orientable_trapdoor_top"),
            Map.entry("block/wooden_trapdoor_bottom", "block/template_orientable_trapdoor_bottom"),
            Map.entry("block/wooden_trapdoor_open", "block/template_orientable_trapdoor_open"),
            Map.entry("block/trapdoor_top", "block/template_orientable_trapdoor_top"),
            Map.entry("block/trapdoor_bottom", "block/template_orientable_trapdoor_bottom"),
            Map.entry("block/trapdoor_open", "block/template_orientable_trapdoor_open"),
            Map.entry("block/orientable_trapdoor_top", "block/template_orientable_trapdoor_top"),
            Map.entry("block/orientable_trapdoor_bottom", "block/template_orientable_trapdoor_bottom"),
            Map.entry("block/orientable_trapdoor_open", "block/template_orientable_trapdoor_open"),
            Map.entry("block/normal_torch", "block/template_torch"),
            Map.entry("block/normal_torch_wall", "block/template_torch_wall"),
            Map.entry("block/tall_grass", "block/short_grass"),
            // from Retromod's model-parent-renames.tsv (MIT), each target checked against the 1.21.1 client jar
            Map.entry("block/glazed_terracotta", "block/template_glazed_terracotta"),
            Map.entry("block/redstone_side", "block/redstone_dust_side"), Map.entry("block/redstone_side_alt", "block/redstone_dust_side_alt"),
            Map.entry("block/fire_floor", "block/template_fire_floor"), Map.entry("block/fire_side", "block/template_fire_side"),
            Map.entry("block/fire_side_alt", "block/template_fire_side_alt"), Map.entry("block/fire_up", "block/template_fire_up"),
            Map.entry("block/fire_up_alt", "block/template_fire_up_alt"), Map.entry("block/torch_wall", "block/template_torch_wall"),
            // iron doors (EnderIO's dark steel door) and the plain rail (Tinkers' wooden rail); the slots are the same
            Map.entry("block/iron_door_bottom", "block/iron_door_bottom_left"), Map.entry("block/iron_door_top", "block/iron_door_top_left"),
            Map.entry("block/iron_door_bottom_rh", "block/iron_door_bottom_right"), Map.entry("block/iron_door_top_rh", "block/iron_door_top_right"),
            Map.entry("block/normal_rail_flat", "block/rail_flat"), Map.entry("block/normal_rail_curved", "block/rail_curved"),
            Map.entry("block/normal_rail_raised_ne", "block/template_rail_raised_ne"), Map.entry("block/normal_rail_raised_sw", "block/template_rail_raised_sw"));

    private static final java.util.regex.Pattern VINE_MODEL = java.util.regex.Pattern.compile("\"minecraft:block/(vine_[a-z0-9_]+)\"");

    /**
     * The 1.12 vine models (a quad of the vine texture 0.8 pixels inside the block per side, plus a ceiling quad for the "u" ones; 1.13 draws the
     * vine from one rotated model instead), as JSON; null for a name that is not one of them. Texture slot "vine".
     */
    static String vineModel(String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("vine_([1-4])?(u)?(_opposite)?").matcher(name);
        if (!m.matches() || (m.group(1) == null && m.group(2) == null)) return null;
        int sides = m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
        boolean up = m.group(2) != null;
        // north, east, south, west planes; vine_2_opposite is north and south
        boolean[] on = new boolean[4];
        if (m.group(3) != null) {
            on[0] = on[2] = true;
        } else {
            for (int i = 0; i < sides; i++) on[i] = true;
        }
        String[] planes = {
                "{\"from\":[0,0,0.8],\"to\":[16,16,0.8],\"shade\":false,\"faces\":{\"north\":{\"uv\":[16,0,0,16],\"texture\":\"#vine\",\"tintindex\":0},\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#vine\",\"tintindex\":0}}}",
                "{\"from\":[15.2,0,0],\"to\":[15.2,16,16],\"shade\":false,\"faces\":{\"west\":{\"uv\":[16,0,0,16],\"texture\":\"#vine\",\"tintindex\":0},\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#vine\",\"tintindex\":0}}}",
                "{\"from\":[0,0,15.2],\"to\":[16,16,15.2],\"shade\":false,\"faces\":{\"north\":{\"uv\":[16,0,0,16],\"texture\":\"#vine\",\"tintindex\":0},\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#vine\",\"tintindex\":0}}}",
                "{\"from\":[0.8,0,0],\"to\":[0.8,16,16],\"shade\":false,\"faces\":{\"west\":{\"uv\":[16,0,0,16],\"texture\":\"#vine\",\"tintindex\":0},\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#vine\",\"tintindex\":0}}}"};
        StringBuilder elements = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (on[i]) elements.append(elements.length() > 0 ? "," : "").append(planes[i]);
        }
        if (up) {
            elements.append(elements.length() > 0 ? "," : "").append("{\"from\":[0,15.2,0],\"to\":[16,15.2,16],\"shade\":false,\"faces\":{\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#vine\",\"tintindex\":0},\"down\":{\"uv\":[0,0,16,16],\"texture\":\"#vine\",\"tintindex\":0}}}");
        }
        return "{\"ambientocclusion\":false,\"textures\":{\"particle\":\"#vine\",\"vine\":\"minecraft:block/vine\"},\"elements\":[" + elements + "]}";
    }

    private static String vanillaModel(String ref) {
        int colon = ref.indexOf(':');
        String ns = colon < 0 ? "minecraft" : ref.substring(0, colon);
        if (!ns.equals("minecraft")) return ref;
        String path = colon < 0 ? ref : ref.substring(colon + 1);
        String renamed = MODEL_RENAMES.get(path);
        if (renamed == null && path.startsWith("block/") && vineModel(path.substring(6)) != null) renamed = path; // generated by addVineModels
        // always with the namespace: a mod that ships its own block/slab_top.json (SRParasites) must not see the renamed parent as itself
        return renamed == null ? ref : "minecraft:" + renamed;
    }

    private static String vanillaTexture(String ref) {
        if (ref.startsWith("#")) return ref;
        int colon = ref.indexOf(':');
        String ns = colon < 0 ? "minecraft" : ref.substring(0, colon);
        if (!ns.equals("minecraft")) return ref;
        String path = colon < 0 ? ref : ref.substring(colon + 1);
        path = VanillaTextureRenames.modern(path);
        return colon < 0 ? path : "minecraft:" + path;
    }

    private static String normalizeRef(String ref) {
        if (ref.startsWith("#")) return ref; // texture variable
        int colon = ref.indexOf(':');
        if (colon < 0) return normalizePath(ref);
        return normalizePath(ref.substring(0, colon)) + ":" + normalizePath(ref.substring(colon + 1));
    }
}
