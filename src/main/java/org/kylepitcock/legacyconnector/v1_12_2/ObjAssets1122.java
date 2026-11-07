package org.kylepitcock.legacyconnector.v1_12_2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.kylepitcock.legacyconnector.core.Ids;

/**
 * Forge 1.12.2 OBJ models named in blockstates and item model registrations ({@code "model": "mod:thing.obj"}) become models of
 * NeoForge's own OBJ loader: a generated JSON model ({@code "loader": "neoforge:obj"}) pointing at the mod's .obj file, with the
 * material library rewritten to modern sprite names (and to the blockstate's texture overrides, which 1.12 keyed by material name).
 * Textures outside the atlas folders are collected in {@link #TEXTURES} for the client to add to the block atlas.
 */
public final class ObjAssets1122 {
    /** Legacy sprite names the OBJ materials use (registered with the block atlas by the client). */
    public static final Set<String> TEXTURES = ConcurrentHashMap.newKeySet();

    private ObjAssets1122() {}

    public static boolean isObj(String path) {
        if (path == null) return false;
        String p = path.toLowerCase(java.util.Locale.ROOT);
        // "x.obj", and the mod-specific variants some mods name their OBJ files ("x.obj.ie" of Immersive Engineering, "x.obj.mek" of Mekanism)
        return p.endsWith(".obj") || p.matches(".*[.]obj[.][a-z0-9]+");
    }

    /**
     * Writes the model for an .obj file and returns its id.
     *
     * @param namespace the modern namespace of the model
     * @param objPath   the path below models/ ("block/thing.obj")
     * @param textures  the 1.12 texture overrides of the variant ("#material" or "material" to a sprite), or null
     * @param custom    the 1.12 custom data ({@code "flip-v": true}), or null
     */
    public static String model(String namespace, String objPath, JsonObject textures, JsonObject custom, String key, Map<String, Supplier<byte[]>> out) {
        String base = "lc1122/obj/" + Ids.normalizePath(objPath.replace('.', '_')) + (key == null ? "" : "_" + Ids.normalizePath(key));
        JsonObject model = new JsonObject();
        model.addProperty("loader", "neoforge:obj");
        model.addProperty("model", namespace + ":models/" + lowercaseMtllib(namespace, objPath, out));
        boolean flip = custom != null && custom.has("flip-v") && custom.get("flip-v").isJsonPrimitive() && custom.get("flip-v").getAsBoolean();
        model.addProperty("flip_v", flip);
        model.addProperty("automatic_culling", false);
        JsonObject modernTextures = new JsonObject();
        if (textures != null) {
            for (var t : textures.entrySet()) {
                if (!t.getValue().isJsonPrimitive()) continue;
                String name = t.getKey().startsWith("#") ? t.getKey().substring(1) : t.getKey();
                String value = t.getValue().getAsString();
                if (!value.startsWith("#")) TEXTURES.add(value);
                modernTextures.addProperty(name, value.startsWith("#") ? value : sprite(value, namespace));
            }
        }
        if (modernTextures.size() > 0) model.add("textures", modernTextures);
        String mtl = rewriteMtl(namespace, objPath, modernTextures);
        if (mtl != null) {
            String mtlPath = "models/" + base + ".mtl";
            byte[] bytes = mtl.getBytes(StandardCharsets.UTF_8);
            out.put("assets/" + namespace + "/" + mtlPath, () -> bytes);
            model.addProperty("mtl_override", namespace + ":" + mtlPath);
        }
        byte[] bytes = model.toString().getBytes(StandardCharsets.UTF_8);
        out.put("assets/" + namespace + "/models/" + base + ".json", () -> bytes);
        return namespace + ":" + base;
    }

    /**
     * NeoForge's OBJ loader turns the "mtllib" name into a resource location, which cannot hold capitals ("mtllib BlockAltar.mtl" of Blood
     * Magic's models failed the whole model). An .obj naming such a library is served as a copy beside it (x.obj.lc) whose line is lower-cased (the
     * material file itself is read as {@code mtl_override}); the path to load is returned.
     */
    private static String lowercaseMtllib(String namespace, String objPath, Map<String, Supplier<byte[]>> out) {
        String obj = read("assets/" + namespace + "/models/" + objPath);
        if (obj == null) return objPath;
        StringBuilder sb = new StringBuilder(obj.length());
        boolean changed = false;
        for (String line : obj.split("\n", -1)) {
            String l = line.trim();
            if (l.startsWith("mtllib ")) {
                String lib = l.substring(7).trim();
                String fixed = Ids.normalizePath(lib);
                if (!fixed.equals(lib)) {
                    line = "mtllib " + fixed;
                    changed = true;
                }
            }
            sb.append(line).append('\n');
        }
        if (!changed) return objPath;
        // next to the original: the library name is resolved against the .obj's folder
        String copy = Ids.normalizePath(objPath) + ".lc";
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        out.put("assets/" + namespace + "/models/" + copy, () -> bytes);
        return copy;
    }

    /** A 1.12 OBJ texture reference as a modern sprite name ("mod:blocks/x", "x" is the model's domain, ".png" and "textures/" dropped). */
    static String sprite(String ref, String namespace) {
        String r = ref.trim();
        int colon = r.indexOf(':');
        String ns = colon < 0 ? namespace : Ids.namespace(r.substring(0, colon));
        String path = colon < 0 ? r : r.substring(colon + 1);
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
        if (path.startsWith("textures/")) path = path.substring("textures/".length());
        return ns + ":" + Ids.normalizePath(path);
    }

    /** The material library of the .obj with each material's texture as a modern sprite (or a texture key the variant overrides). */
    private static String rewriteMtl(String namespace, String objPath, JsonObject overrides) {
        String obj = read("assets/" + namespace + "/models/" + objPath);
        if (obj == null) return null;
        String lib = null;
        for (String line : obj.split("\n")) {
            String l = line.trim();
            if (l.startsWith("mtllib ")) {
                lib = l.substring(7).trim();
                break;
            }
        }
        if (lib == null) return null;
        int slash = objPath.lastIndexOf('/');
        String dir = slash < 0 ? "" : objPath.substring(0, slash + 1);
        String mtl = lib.contains(":") ? read("assets/" + Ids.namespace(lib.substring(0, lib.indexOf(':'))) + "/" + lib.substring(lib.indexOf(':') + 1))
                : read("assets/" + namespace + "/models/" + dir + lib);
        // Forge 1.12 took a library name with a folder as a path under the domain ("mtllib models/block/x.mtl")
        if (mtl == null && !lib.contains(":") && lib.contains("/")) mtl = read("assets/" + namespace + "/" + lib);
        // the jar's files carry lower-case names, the .obj still names the library with its original capitals
        if (mtl == null && !lib.contains(":")) mtl = read("assets/" + namespace + "/models/" + dir + Ids.normalizePath(lib));
        if (mtl == null) return null;
        StringBuilder sb = new StringBuilder();
        String material = null;
        // Forge 1.12 took a material's texture from map_Ka as well as map_Kd (Immersive Engineering's libraries only have map_Ka); NeoForge reads map_Kd
        java.util.Set<String> diffuse = new java.util.HashSet<>();
        String scanned = null;
        for (String line : mtl.split("\n")) {
            String l = line.trim();
            if (l.startsWith("newmtl ")) scanned = l.substring(7).trim();
            else if (l.startsWith("map_Kd ") && scanned != null) diffuse.add(scanned);
        }
        for (String line : mtl.split("\n")) {
            String l = line.trim();
            if (l.startsWith("newmtl ")) {
                material = l.substring(7).trim();
                sb.append(l).append('\n');
            } else if (l.startsWith("map_Kd ") || (l.startsWith("map_Ka ") && material != null && !diffuse.contains(material))) {
                String ref = l.substring(7).trim();
                String refKey = ref.startsWith("#") ? ref.substring(1) : ref;
                if (material != null && overrides.has(material)) {
                    sb.append("map_Kd #").append(material).append('\n');
                } else if (overrides.has(refKey)) {
                    // Forge 1.12 also matched the override key against the texture path the material names ("#node" for "map_Kd node")
                    sb.append("map_Kd #").append(refKey).append('\n');
                } else if (ref.startsWith("#")) {
                    sb.append("map_Kd ").append(ref).append('\n');
                } else {
                    TEXTURES.add(ref.contains(":") ? ref : namespace + ":" + ref);
                    sb.append("map_Kd ").append(sprite(ref, namespace)).append('\n');
                }
            } else {
                sb.append(l).append('\n');
            }
        }
        return sb.toString();
    }

    private static String read(String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = loader.getResourceAsStream(path)) {
            if (in == null) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** The custom data object of a variant, or null. */
    static JsonObject custom(JsonObject variant) {
        JsonElement c = variant.get("custom");
        return c != null && c.isJsonObject() ? c.getAsJsonObject() : null;
    }
}
