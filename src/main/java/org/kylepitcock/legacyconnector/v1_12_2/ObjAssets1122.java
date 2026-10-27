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
        return path != null && path.toLowerCase(java.util.Locale.ROOT).endsWith(".obj");
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
        model.addProperty("model", namespace + ":models/" + objPath);
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
        if (mtl == null) return null;
        StringBuilder sb = new StringBuilder();
        String material = null;
        for (String line : mtl.split("\n")) {
            String l = line.trim();
            if (l.startsWith("newmtl ")) {
                material = l.substring(7).trim();
                sb.append(l).append('\n');
            } else if (l.startsWith("map_Kd ")) {
                String ref = l.substring(7).trim();
                if (material != null && overrides.has(material)) {
                    sb.append("map_Kd #").append(material).append('\n');
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
