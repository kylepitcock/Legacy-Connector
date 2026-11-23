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
        // an OBJ variant: written with the generated assets and baked by the game as a standalone model (Assets1122.orphanModel)
        var generated = org.kylepitcock.legacyconnector.v1_12_2.Assets1122.orphanModel(location.toString());
        if (generated != null) {
            var key = net.minecraft.client.resources.model.ModelResourceLocation.standalone(generated);
            var baking = CustomModels1122.baking();
            var modern = baking != null ? baking.get(key) : Minecraft.getInstance().getModelManager().getModel(key);
            if (modern != null && modern != Models1122.missingModern()) return Models1122.legacy(modern);
        }
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

    /**
     * A variant whose model is a Forge OBJ file, baked by the shim's OBJ loader with the caller's texture getter, as 1.12's variant loader did:
     * the defaults' and the variant's textures and custom data (flip-v) applied. For variants a mod asks for only while the models are baked
     * (Techguns loads its 40mm grenade's {@code techguns:grenade40mm#inventory} in ModelBakeEvent, after the generated assets were written, so no
     * standalone model of it exists: it drew the missing model, whose quads all have a side, and its entity renderer drew nothing). Null when the
     * variant does not name an OBJ file.
     */
    public static IBakedModel bakeObj(ModelResourceLocation location,
            legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState state,
            legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat format,
            java.util.function.Function<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation,
                    legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite> getter) {
        try {
            String ns = location.getNamespace();
            JsonObject file = read("assets/" + ns + "/blockstates/" + location.getPath() + ".json");
            if (file == null || !file.has("variants") || !file.get("variants").isJsonObject()) return null;
            JsonElement found = file.getAsJsonObject("variants").get(location.getVariant());
            if (found == null && "normal".equals(location.getVariant())) found = file.getAsJsonObject("variants").get("");
            while (found != null && found.isJsonArray()) found = found.getAsJsonArray().isEmpty() ? null : found.getAsJsonArray().get(0);
            if (found == null || !found.isJsonObject()) return null;
            JsonObject variant = found.getAsJsonObject();
            JsonObject defaults = file.has("defaults") && file.get("defaults").isJsonObject() ? file.getAsJsonObject("defaults") : new JsonObject();
            String model = string(variant, "model");
            if (model == null) model = string(defaults, "model");
            if (model == null || !org.kylepitcock.legacyconnector.v1_12_2.ObjAssets1122.isObj(model)) return null;
            int colon = model.indexOf(':');
            String modelNs = colon < 0 ? ns : model.substring(0, colon);
            String name = colon < 0 ? model : model.substring(colon + 1);
            // the variant loader's folder is models/block/ (the OBJ loader also looks in models/item/)
            String path = name.startsWith("block/") || name.startsWith("item/") ? name : "block/" + name;
            legacyconnector.v1_12_2.net.minecraftforge.client.model.IModel obj = legacyconnector.v1_12_2.net.minecraftforge.client.model.obj.OBJLoader.INSTANCE
                    .loadModel(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(modelNs, "models/" + path));
            var textures = new java.util.LinkedHashMap<String, String>();
            var custom = new java.util.LinkedHashMap<String, String>();
            for (var source : new JsonObject[] {defaults, variant}) {
                if (source.has("textures") && source.get("textures").isJsonObject()) {
                    for (var t : source.getAsJsonObject("textures").entrySet()) if (t.getValue().isJsonPrimitive()) textures.put(t.getKey(), t.getValue().getAsString());
                }
                if (source.has("custom") && source.get("custom").isJsonObject()) {
                    for (var c : source.getAsJsonObject("custom").entrySet()) custom.put(c.getKey(), c.getValue().toString());
                }
            }
            if (!textures.isEmpty()) obj = obj.retexture(com.google.common.collect.ImmutableMap.copyOf(textures));
            if (!custom.isEmpty()) obj = obj.process(com.google.common.collect.ImmutableMap.copyOf(custom));
            var bakeGetter = getter != null ? getter : legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.defaultTextureGetter();
            return obj.bake(state != null ? state : obj.getDefaultState(), format, bakeGetter);
        } catch (Exception | LinkageError e) {
            RenderErrors1122.report("baking the OBJ model of the blockstate variant " + location, e);
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
