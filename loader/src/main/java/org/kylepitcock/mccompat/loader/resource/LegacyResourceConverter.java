package org.kylepitcock.mccompat.loader.resource;

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
 *     under {@code mccompat_legacy/} for the runtime translator.</li>
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
            output.putIfAbsent("mccompat_legacy/" + modern, data);
            return true;
        }
        if (modern.endsWith(".json") && (modern.contains("/models/") || modern.contains("/blockstates/"))) {
            data = lowercaseResourceRefs(data);
        }
        output.putIfAbsent(modern, data);
        return true;
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

    static void parseLang(String text, Map<String, String> out) {
        boolean escapes = false;
        for (String line : text.split("\\r?\\n")) {
            if (line.startsWith("﻿")) line = line.substring(1);
            if (line.trim().equals("#PARSE_ESCAPES")) {
                escapes = true;
                continue;
            }
            if (line.isBlank() || line.startsWith("#") || line.startsWith("//")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String value = line.substring(eq + 1);
            if (escapes) value = value.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
            // 1.7.10 used Java format specifiers verbatim (%s, %d); modern Component formatting accepts %s and
            // positional %1$s only. Numeric conversions are rewritten so translations do not render raw.
            value = value.replaceAll("%(\\d+\\$)?[dfx]", "%$1s").replaceAll("%(\\d+\\$)?\\.\\d+f", "%$1s");
            out.put(line.substring(0, eq), value);
        }
    }

    /** Model/blockstate JSON written for 1.7.10-1.12 may reference textures and parents with upper case. */
    static byte[] lowercaseResourceRefs(byte[] data) {
        try {
            JsonElement root = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
            fixRefs(root, null);
            return GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return data; // keep malformed JSON byte-identical; the vanilla loader will report it
        }
    }

    private static void fixRefs(JsonElement element, String key) {
        if (element instanceof JsonObject obj) {
            for (var e : obj.entrySet()) {
                if (e.getValue() instanceof JsonPrimitive p && p.isString() && isRefKey(e.getKey(), key)) {
                    e.setValue(new JsonPrimitive(normalizeRef(p.getAsString())));
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

    private static String normalizeRef(String ref) {
        if (ref.startsWith("#")) return ref; // texture variable
        int colon = ref.indexOf(':');
        if (colon < 0) return normalizePath(ref);
        return normalizePath(ref.substring(0, colon)) + ":" + normalizePath(ref.substring(colon + 1));
    }
}
