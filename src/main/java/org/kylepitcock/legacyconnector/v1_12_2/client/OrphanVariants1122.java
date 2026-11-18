package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Models of blockstate files no block owns. In 1.12 a mod could ask for {@code new ModelResourceLocation("botania:jadedamaranthus", "normal")}
 * although the name is no block (Botania's special flowers are one block with a subtile per name; their models are the blockstates of the
 * subtiles' names) and the game baked the variant of that file. Here only the files of registered blocks are converted to modern blockstates, so
 * such a location has no baked model; this bakes the variant straight from the mod's own legacy blockstate json (its model, textures and
 * rotation) the way the game bakes a modern variant.
 */
public final class OrphanVariants1122 {
    private static final Map<String, java.util.Optional<IBakedModel>> CACHE = new ConcurrentHashMap<>();

    private OrphanVariants1122() {}

    /** Forgets the baked variants (the models were reloaded). */
    public static void clear() {
        CACHE.clear();
    }

    /** The baked model of a legacy blockstate variant, or null when the file, the variant or its model does not exist. */
    public static IBakedModel bake(ModelResourceLocation location) {
        return CACHE.computeIfAbsent(location.toString(), key -> java.util.Optional.ofNullable(bakeNow(location))).orElse(null);
    }

    private static IBakedModel bakeNow(ModelResourceLocation location) {
        try {
            String ns = location.getNamespace();
            String path = location.getPath();
            JsonObject file = read("assets/" + ns + "/blockstates/" + path + ".json");
            if (file == null || !file.has("variants") || !file.get("variants").isJsonObject()) return null;
            JsonObject variants = file.getAsJsonObject("variants");
            String wanted = location.getVariant();
            JsonElement found = variants.get(wanted);
            if (found == null && wanted.equals("normal")) found = variants.get("");
            if (found == null) return null;
            while (found.isJsonArray()) {
                if (found.getAsJsonArray().isEmpty()) return null;
                found = found.getAsJsonArray().get(0);
            }
            if (!found.isJsonObject()) return null;
            JsonObject variant = found.getAsJsonObject();
            JsonObject defaults = file.has("defaults") && file.get("defaults").isJsonObject() ? file.getAsJsonObject("defaults") : new JsonObject();
            String model = string(variant, "model");
            if (model == null) model = string(defaults, "model");
            if (model == null) return null;
            ResourceLocation parent = parentOf(ns, model);
            if (parent == null) return null;
            JsonObject json = new JsonObject();
            json.addProperty("parent", parent.toString());
            JsonObject textures = new JsonObject();
            for (var source : new JsonObject[] {defaults, variant}) {
                if (!source.has("textures") || !source.get("textures").isJsonObject()) continue;
                for (var t : source.getAsJsonObject("textures").entrySet()) {
                    if (t.getValue().isJsonPrimitive()) textures.addProperty(t.getKey(), modernTexture(ns, t.getValue().getAsString()));
                }
            }
            if (!textures.isEmpty()) json.add("textures", textures);
            int x = variant.has("x") && variant.get("x").isJsonPrimitive() ? variant.get("x").getAsInt() : 0;
            int y = variant.has("y") && variant.get("y").isJsonPrimitive() ? variant.get("y").getAsInt() : 0;
            return JsonBake1122.bakeVariant(json.toString(), x, y);
        } catch (RuntimeException e) {
            RenderErrors1122.report("baking the blockstate variant " + location, e);
            return null;
        }
    }

    /** The modern model location of a legacy variant's model name, or null for the ones code draws. */
    private static ResourceLocation parentOf(String ns, String model) {
        if (model.equals("builtin/generated") || model.equals("minecraft:builtin/generated") || model.equals("item/generated")) return ResourceLocation.withDefaultNamespace("item/generated");
        if (model.contains("builtin/")) return null;
        int colon = model.indexOf(':');
        String modelNs = colon < 0 ? ns : model.substring(0, colon);
        String name = colon < 0 ? model : model.substring(colon + 1);
        var resources = Minecraft.getInstance().getResourceManager();
        for (String candidate : new String[] {"block/" + name, name, "item/" + name}) {
            var id = ResourceLocation.fromNamespaceAndPath(modelNs, candidate);
            if (resources.getResource(id.withPrefix("models/").withSuffix(".json")).isPresent()) return id;
        }
        return null;
    }

    private static String modernTexture(String ns, String ref) {
        if (ref.startsWith("#")) return ref;
        int colon = ref.indexOf(':');
        String textureNs = colon < 0 ? ns : ref.substring(0, colon);
        String path = colon < 0 ? ref : ref.substring(colon + 1);
        if (path.startsWith("blocks/")) path = "block/" + path.substring(7);
        else if (path.startsWith("items/")) path = "item/" + path.substring(6);
        return textureNs + ":" + path;
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    private static JsonObject read(String path) {
        try (var in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in == null) return null;
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }
}
