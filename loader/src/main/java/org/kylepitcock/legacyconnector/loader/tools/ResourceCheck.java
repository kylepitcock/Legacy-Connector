package org.kylepitcock.legacyconnector.loader.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Offline rendering smoke test of a translated jar, without starting the game: the purple-and-black "missing texture" and the invisible
 * block are the most common rendering failures of translated mods, and nearly all of them are references that point at nothing. Over the
 * converted assets of a jar (what the loader produced, with the 1.12 folders and names already moved) it checks that
 * <ul>
 *   <li>every model's {@code parent} exists (in the jar, in another jar of the same set, or in vanilla 1.21.1),</li>
 *   <li>every literal texture a model names exists as {@code textures/<path>.png} (same three places),</li>
 *   <li>every model a blockstate variant or multipart part names exists,</li>
 *   <li>every {@code #variable} that an element face of a block or item model uses resolves to a texture through the parent chain,</li>
 *   <li>JSON files parse and PNG files have a readable header.</li>
 * </ul>
 * It cannot see what a texture looks like, animations, lighting, translucency order or anything a renderer draws in code.
 */
public final class ResourceCheck {
    private static final int MAX_EXAMPLES = 10;
    private static final int MAX_CHAIN = 16;

    private ResourceCheck() {}

    /** Names of the {@code assets/...} entries of a set of jars (the jars of one probe run plus vanilla), and a small cache of parsed JSON. */
    public static final class Index implements AutoCloseable {
        private final List<Path> jars = new ArrayList<>();
        private final Map<String, Integer> where = new HashMap<>();
        private final Map<Integer, ZipFile> open = new java.util.LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, ZipFile> eldest) {
                if (size() <= 24) return false;
                try {
                    eldest.getValue().close();
                } catch (IOException ignored) {
                    // closing a cache entry
                }
                return true;
            }
        };
        private final Map<String, JsonObject> models = new HashMap<>();
        private final Set<String> namespaces = new HashSet<>();
        /** Lower-cased {@code assets/} entry names of the ORIGINAL (untranslated) jars: what the mods themselves ship. */
        private final Set<String> original = new HashSet<>();

        /** Adds the asset names of an original, untranslated jar, to tell what a mod never shipped from what the translation lost. */
        public void addOriginal(Path jar) throws IOException {
            try (ZipFile z = new ZipFile(jar.toFile())) {
                Enumeration<? extends ZipEntry> en = z.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    String name = e.getName();
                    if (!e.isDirectory() && name.startsWith("assets/")) original.add(name.toLowerCase(java.util.Locale.ROOT));
                }
            }
        }

        /** How an unresolved asset file counts: {@code SKIP} (its namespace belongs to a mod that is not in the set), {@code MOD} (no original jar ships it, a defect of the mod itself) or {@code LAYER} (shipped, or a vanilla file, but not there after translation). */
        String verdict(String file) {
            String ns = file.substring("assets/".length(), file.indexOf('/', "assets/".length()));
            if (ns.equals("minecraft")) return "LAYER";
            if (!namespaces.contains(ns)) return "SKIP";
            return original.contains(file) ? "LAYER" : "MOD";
        }

        /** Adds the {@code assets/} entries of a jar; an entry another jar already provides keeps the first. */
        public void add(Path jar) throws IOException {
            int id = jars.size();
            jars.add(jar);
            try (ZipFile z = new ZipFile(jar.toFile())) {
                Enumeration<? extends ZipEntry> en = z.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    String name = e.getName();
                    if (!e.isDirectory() && name.startsWith("assets/") && name.indexOf('/', "assets/".length()) > 0) {
                        where.putIfAbsent(name, id);
                        namespaces.add(name.substring("assets/".length(), name.indexOf('/', "assets/".length())));
                    }
                }
            }
        }

        boolean has(String name) {
            return where.containsKey(name);
        }

        byte[] read(String name) {
            Integer id = where.get(name);
            if (id == null) return null;
            try {
                ZipFile z = open.get(id);
                if (z == null) open.put(id, z = new ZipFile(jars.get(id).toFile()));
                ZipEntry e = z.getEntry(name);
                if (e == null) return null;
                try (InputStream in = z.getInputStream(e)) {
                    return in.readAllBytes();
                }
            } catch (IOException | RuntimeException e) {
                return null;
            }
        }

        /** A parsed model, null when absent or unreadable. Vanilla and other jars' models are cached, there are few of them. */
        JsonObject model(String path) {
            JsonObject m = models.get(path);
            if (m != null) return m;
            byte[] b = read(path);
            if (b == null) return null;
            try {
                JsonElement e = JsonParser.parseString(new String(b, StandardCharsets.UTF_8));
                if (!e.isJsonObject()) return null;
                if (models.size() > 20000) models.clear();
                models.put(path, e.getAsJsonObject());
                return e.getAsJsonObject();
            } catch (RuntimeException e) {
                return null;
            }
        }

        @Override
        public void close() {
            for (ZipFile z : open.values()) {
                try {
                    z.close();
                } catch (IOException ignored) {
                    // closing
                }
            }
            open.clear();
        }
    }

    /** Counts of one jar: every unresolved reference is a texture or a model problem, and either the layer's or the mod's own (see {@link Index#verdict}). */
    private static final class Tally {
        int textures, models, layer, mod, badJson, badPng;
        final Set<String> seen = new HashSet<>();
        final List<String> examples = new ArrayList<>();

        void problem(Index index, boolean texture, String file, String key, String text) {
            String verdict = index.verdict(file);
            if (verdict.equals("SKIP") || !seen.add(key)) return;
            if (texture) textures++;
            else models++;
            if (verdict.equals("LAYER")) layer++;
            else mod++;
            // the layer's problems first: they are the ones that can be fixed here
            String tagged = (verdict.equals("LAYER") ? "[layer] " : "[mod] ") + text;
            if (verdict.equals("LAYER")) examples.add(Math.min(examples.size(), 0), tagged);
            else if (examples.size() < MAX_EXAMPLES) examples.add(tagged);
            while (examples.size() > MAX_EXAMPLES) examples.remove(examples.size() - 1);
        }
    }

    /** Result counts and the first few problems of one jar, as the "assets" object of the probe record. */
    public static JsonObject check(Path modJar, Index index) throws IOException {
        int modelCount = 0, stateCount = 0, textureCount = 0;
        Tally t = new Tally();
        Set<String> usedModels = new LinkedHashSet<>(); // models a blockstate or the item folder uses directly: their variables must resolve
        try (ZipFile z = new ZipFile(modJar.toFile())) {
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String name = e.getName();
                if (e.isDirectory() || !name.startsWith("assets/") || name.indexOf('/', "assets/".length()) < 0) continue;
                if (name.endsWith(".png") && name.contains("/textures/")) {
                    textureCount++;
                    if (!pngHeaderOk(z, e) && t.seen.add("png " + name)) {
                        t.badPng++;
                        example(t.examples, "[mod] unreadable png " + name);
                    }
                } else if (name.endsWith(".json") && name.contains("/models/")) {
                    modelCount++;
                    JsonObject m = parse(z, e);
                    if (m == null) {
                        if (t.seen.add("json " + name)) {
                            t.badJson++;
                            example(t.examples, "[mod] unparseable json " + name);
                        }
                        continue;
                    }
                    String ns = name.substring("assets/".length(), name.indexOf('/', "assets/".length()));
                    if (name.contains("/models/item/")) usedModels.add(name);
                    if (m.has("parent") && m.get("parent").isJsonPrimitive()) {
                        String parent = m.get("parent").getAsString();
                        if (!isBuiltin(parent) && !index.has(modelFile(parent, ns))) {
                            t.problem(index, false, modelFile(parent, ns), "parent " + name + parent, "model " + shortName(name) + " has missing parent " + parent);
                        }
                    }
                    if (m.has("overrides") && m.get("overrides").isJsonArray()) {
                        for (JsonElement o : m.getAsJsonArray("overrides")) {
                            if (!o.isJsonObject() || !o.getAsJsonObject().has("model") || !o.getAsJsonObject().get("model").isJsonPrimitive()) continue;
                            String ref = o.getAsJsonObject().get("model").getAsString();
                            if (!index.has(modelFile(ref, ns)) && !index.has(blockModelFile(ref, ns))) {
                                t.problem(index, false, modelFile(ref, ns), "override " + name + ref, "model " + shortName(name) + " has an override to missing model " + ref);
                            }
                        }
                    }
                    if (m.has("textures") && m.get("textures").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> tex : m.getAsJsonObject("textures").entrySet()) {
                            if (!tex.getValue().isJsonPrimitive()) continue;
                            String ref = tex.getValue().getAsString();
                            if (ref.startsWith("#")) continue;
                            if (!index.has(textureFile(ref, ns))) {
                                t.problem(index, true, textureFile(ref, ns), "tex " + name + ref, "model " + shortName(name) + " names missing texture " + ref);
                            }
                        }
                    }
                } else if (name.endsWith(".json") && name.contains("/blockstates/")) {
                    stateCount++;
                    JsonObject s = parse(z, e);
                    if (s == null) {
                        if (t.seen.add("json " + name)) {
                            t.badJson++;
                            example(t.examples, "[mod] unparseable json " + name);
                        }
                        continue;
                    }
                    if (s.has("forge_marker")) continue; // Forge's own format: translated at runtime, not readable offline
                    String ns = name.substring("assets/".length(), name.indexOf('/', "assets/".length()));
                    for (String ref : stateModels(s)) {
                        String file = modelFile(ref, ns);
                        String block = blockModelFile(ref, ns); // 1.12 named block models relative to models/block/ of the blockstate's namespace
                        if (index.has(file)) usedModels.add(file);
                        else if (index.has(block)) usedModels.add(block);
                        else t.problem(index, false, block, "state " + name + ref, "blockstate " + shortName(name) + " names missing model " + ref);
                    }
                }
            }
        }
        for (String used : usedModels) {
            String[] problem = unresolvedVariable(used, index);
            if (problem != null) t.problem(index, true, problem[1], "var " + used, "model " + shortName(used) + " uses " + problem[0]);
        }
        JsonObject r = new JsonObject();
        r.addProperty("models", modelCount);
        r.addProperty("blockstates", stateCount);
        r.addProperty("textures", textureCount);
        r.addProperty("missingTextures", t.textures);
        r.addProperty("missingModels", t.models);
        r.addProperty("layer", t.layer);   // unresolved because of the translation (or a vanilla file the rename tables miss)
        r.addProperty("mod", t.mod);       // unresolved in the mod's own jar too
        r.addProperty("badJson", t.badJson);
        r.addProperty("badPng", t.badPng);
        JsonArray ex = new JsonArray();
        t.examples.forEach(ex::add);
        r.add("examples", ex);
        return r;
    }

    private static void example(List<String> examples, String text) {
        if (examples.size() < MAX_EXAMPLES) examples.add(text);
    }

    private static String shortName(String entry) {
        return entry.replaceFirst("^assets/", "").replace("/models/", ":").replace("/blockstates/", ":");
    }

    private static boolean isBuiltin(String parent) {
        String p = parent.startsWith("minecraft:") ? parent.substring("minecraft:".length()) : parent;
        return p.startsWith("builtin/");
    }

    /** {@code assets/<ns>/models/<path>.json} of a model reference, the namespace defaulting to {@code defaultNs}. */
    private static String modelFile(String ref, String defaultNs) {
        int c = ref.indexOf(':');
        String ns = c < 0 ? "minecraft" : ref.substring(0, c);
        String path = c < 0 ? ref : ref.substring(c + 1);
        // a bare name is in the minecraft namespace, as in the game (1.12 and later)
        return "assets/" + ns.toLowerCase(java.util.Locale.ROOT) + "/models/" + path.toLowerCase(java.util.Locale.ROOT) + ".json";
    }

    /** {@code assets/<ns>/models/block/<name>.json} for a 1.12 blockstate model name ({@code name} or {@code ns:name}; a bare name is vanilla's). */
    private static String blockModelFile(String ref, String defaultNs) {
        int c = ref.indexOf(':');
        String ns = c < 0 ? "minecraft" : ref.substring(0, c); // 1.12's blockstate deserializer: new ResourceLocation(name), then "block/" + path
        String path = c < 0 ? ref : ref.substring(c + 1);
        if (!path.startsWith("block/")) path = "block/" + path;
        return "assets/" + ns.toLowerCase(java.util.Locale.ROOT) + "/models/" + path.toLowerCase(java.util.Locale.ROOT) + ".json";
    }

    private static String textureFile(String ref, String defaultNs) {
        int c = ref.indexOf(':');
        String ns = c < 0 ? "minecraft" : ref.substring(0, c);
        String path = c < 0 ? ref : ref.substring(c + 1);
        return "assets/" + ns.toLowerCase(java.util.Locale.ROOT) + "/textures/" + path.toLowerCase(java.util.Locale.ROOT) + ".png";
    }

    private static JsonObject parse(ZipFile z, ZipEntry e) {
        try (InputStream in = z.getInputStream(e)) {
            JsonElement el = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private static boolean pngHeaderOk(ZipFile z, ZipEntry e) {
        try (InputStream in = z.getInputStream(e)) {
            byte[] h = in.readNBytes(24);
            if (h.length < 24 || (h[0] & 0xFF) != 0x89 || h[1] != 'P' || h[2] != 'N' || h[3] != 'G') return false;
            int w = ((h[16] & 0xFF) << 24) | ((h[17] & 0xFF) << 16) | ((h[18] & 0xFF) << 8) | (h[19] & 0xFF);
            int hgt = ((h[20] & 0xFF) << 24) | ((h[21] & 0xFF) << 16) | ((h[22] & 0xFF) << 8) | (h[23] & 0xFF);
            return w > 0 && hgt > 0;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    /** The model references of a vanilla-format blockstate (variants, with a list of weighted models, and multipart). */
    private static List<String> stateModels(JsonObject s) {
        List<String> out = new ArrayList<>();
        if (s.has("variants") && s.get("variants").isJsonObject()) {
            for (JsonElement v : s.getAsJsonObject("variants").asMap().values()) collect(v, out);
        }
        if (s.has("multipart") && s.get("multipart").isJsonArray()) {
            for (JsonElement part : s.getAsJsonArray("multipart")) {
                if (part.isJsonObject() && part.getAsJsonObject().has("apply")) collect(part.getAsJsonObject().get("apply"), out);
            }
        }
        return out;
    }

    private static void collect(JsonElement v, List<String> out) {
        if (v.isJsonArray()) {
            for (JsonElement x : v.getAsJsonArray()) collect(x, out);
        } else if (v.isJsonObject() && v.getAsJsonObject().has("model") && v.getAsJsonObject().get("model").isJsonPrimitive()) {
            out.add(v.getAsJsonObject().get("model").getAsString());
        }
    }

    /**
     * A description of the first element-face (or particle-less) {@code #variable} a used model cannot resolve through its parent chain, or null
     * when everything resolves. The chain stops at a builtin parent; elements come from the nearest model that has any.
     */
    private static String[] unresolvedVariable(String modelEntry, Index index) {
        String ns = modelEntry.substring("assets/".length(), modelEntry.indexOf('/', "assets/".length()));
        Map<String, String> textures = new HashMap<>();
        JsonArray elements = null;
        String current = modelEntry;
        for (int depth = 0; depth < MAX_CHAIN && current != null; depth++) {
            JsonObject m = index.model(current);
            if (m == null) return null; // an absent or unreadable model is reported elsewhere
            if (m.has("textures") && m.get("textures").isJsonObject()) {
                for (Map.Entry<String, JsonElement> t : m.getAsJsonObject("textures").entrySet()) {
                    if (t.getValue().isJsonPrimitive()) textures.putIfAbsent(t.getKey(), t.getValue().getAsString());
                }
            }
            if (elements == null && m.has("elements") && m.get("elements").isJsonArray()) elements = m.getAsJsonArray("elements");
            String parent = m.has("parent") && m.get("parent").isJsonPrimitive() ? m.get("parent").getAsString() : null;
            if (parent == null || isBuiltin(parent)) break;
            String curNs = current.substring("assets/".length(), current.indexOf('/', "assets/".length()));
            current = modelFile(parent, curNs);
            if (!index.has(current)) return null;
        }
        if (elements == null) return null;
        for (JsonElement el : elements) {
            if (!el.isJsonObject() || !el.getAsJsonObject().has("faces") || !el.getAsJsonObject().get("faces").isJsonObject()) continue;
            for (JsonElement face : el.getAsJsonObject().getAsJsonObject("faces").asMap().values()) {
                if (!face.isJsonObject() || !face.getAsJsonObject().has("texture")) continue;
                String ref = face.getAsJsonObject().get("texture").getAsString();
                if (!ref.startsWith("#")) continue;
                String resolved = ref;
                for (int i = 0; i < 12 && resolved.startsWith("#"); i++) {
                    String next = textures.get(resolved.substring(1));
                    if (next == null) return new String[] {"undefined texture variable " + resolved, "assets/" + ns + "/textures/undefined-variable"};
                    resolved = next;
                }
                if (resolved.startsWith("#")) return new String[] {"circular texture variable " + ref, "assets/" + ns + "/textures/undefined-variable"};
                if (!index.has(textureFile(resolved, ns))) return new String[] {"missing texture " + resolved + " (via " + ref + ")", textureFile(resolved, ns)};
            }
        }
        return null;
    }
}
