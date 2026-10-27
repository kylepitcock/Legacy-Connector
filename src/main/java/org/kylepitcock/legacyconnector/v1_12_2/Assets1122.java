package org.kylepitcock.legacyconnector.v1_12_2;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generated client assets for 1.12.2 blocks: a modern block keeps its legacy state as one META property, so each block's own
 * blockstate file (variants keyed by property values, including Forge's nested form) is rewritten to variants keyed by {@code meta=N}.
 */
public final class Assets1122 implements GeneratedPack.Contributor {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Assets");
    private static final Gson GSON = new Gson();

    @Override
    public void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.CLIENT_RESOURCES) return;
        itemModels(out);
        for (Block block : Registries1122.BLOCKS.getValues()) {
            if (!(block.mcc$modern instanceof Legacy1122Block modern)) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(modern);
            try {
                JsonObject state = translate(block, id, out);
                if (state == null) continue;
                byte[] bytes = GSON.toJson(state).getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json", () -> bytes);
                blockItemModels(block, id, state, out);
                if (System.getProperty("legacyconnector.debugAssets") != null && id.getPath().equals(System.getProperty("legacyconnector.debugAssets"))) LOG.info("blockstate {}: {}", id, GSON.toJson(state));
            } catch (RuntimeException e) {
                LOG.debug("Could not translate the blockstate of {}: {}", id, e.toString());
            }
        }
    }

    /** Item models registered in code (ModelLoader): each subtype's modern item points at the model file the mod named. */
    private static void itemModels(Map<String, Supplier<byte[]>> out) {
        for (legacyconnector.v1_12_2.net.minecraft.item.Item item : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems()) {
            if (item.getRegistryName() == null || item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBlock) continue;
            for (int meta = 0; meta < 16; meta++) {
                var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modern(item, meta);
                if (modern == null) continue;
                var mrl = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$model(item, meta);
                if (mrl == null) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(modern);
                String target = org.kylepitcock.legacyconnector.core.Ids.namespace(mrl.getNamespace()) + ":item/" + org.kylepitcock.legacyconnector.core.Ids.normalizePath(mrl.getPath());
                if (ObjAssets1122.isObj(mrl.getPath())) {
                    // an OBJ item model (models/item/<name>.obj): NeoForge's OBJ loader
                    target = ObjAssets1122.model(org.kylepitcock.legacyconnector.core.Ids.namespace(mrl.getNamespace()),
                            "item/" + org.kylepitcock.legacyconnector.core.Ids.normalizePath(mrl.getPath()), null, null, null, out);
                }
                if (target.equals(id.getNamespace() + ":item/" + id.getPath())) continue; // the mod's own file already has the id's name
                JsonObject model = new JsonObject();
                model.addProperty("parent", target);
                byte[] bytes = GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", () -> bytes);
            }
        }
    }

    private static JsonObject translate(Block block, ResourceLocation id, Map<String, Supplier<byte[]>> out) {
        JsonObject original = readOriginal(id);
        if (original == null) return null; // the mod models this block in code (or not at all): nothing to translate
        JsonObject legacyVariants = original != null && original.has("variants") && original.get("variants").isJsonObject() ? original.getAsJsonObject("variants") : null;
        JsonObject defaults = original.has("defaults") && original.get("defaults").isJsonObject() ? original.getAsJsonObject("defaults") : null;
        JsonObject variants = new JsonObject();
        List<IBlockState> states = new ArrayList<>(block.getBlockState().getValidStates());
        if (System.getProperty("legacyconnector.debugAssets") != null && id.getPath().equals(System.getProperty("legacyconnector.debugAssets"))) {
            StringBuilder sb = new StringBuilder();
            for (IBlockState st : states) sb.append(st.getProperties().entrySet().stream().map(e -> e.getKey().getName() + "=" + nameOf(e.getKey(), e.getValue())).collect(java.util.stream.Collectors.joining(","))).append(" | ");
            LOG.info("states of {}: {} legacyVariants={}", id, sb, legacyVariants == null ? null : legacyVariants.keySet());
        }
        for (IBlockState state : states) {
            int meta;
            try {
                meta = block.getMetaFromState(state) & 15;
            } catch (RuntimeException e) {
                meta = 0;
            }
            String modernKey = "meta=" + meta;
            if (variants.has(modernKey)) continue;
            JsonElement model = legacyVariants == null ? null : find(legacyVariants, state);
            if (model == null) model = fromStateMapper(block, id, state);
            if (model != null) variants.add(modernKey, blockModels(effective(model, defaults), id, modernKey.replace('=', '_'), out));
        }
        if (variants.size() == 0) {
            // no usable legacy file: a cube whose model is named like the block
            JsonObject m = new JsonObject();
            m.addProperty("model", id.getNamespace() + ":block/" + id.getPath());
            variants.add("meta=0", m);
        }
        // metadata values no legacy state maps to (the block saves only some of the 16): they show the first state's model instead of a missing-model cube
        if (variants.size() > 0) {
            JsonElement fallback = variants.has("meta=0") ? variants.get("meta=0") : variants.entrySet().iterator().next().getValue();
            for (int meta = 0; meta < 16; meta++) if (!variants.has("meta=" + meta)) variants.add("meta=" + meta, fallback);
        }
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    /** Forge's blockstate format v1: a variant inherits the "defaults" object (its textures are merged, the other keys replaced). */
    private static JsonElement effective(JsonElement variant, JsonObject defaults) {
        if (variant.isJsonArray()) {
            var arr = new com.google.gson.JsonArray();
            for (JsonElement e : variant.getAsJsonArray()) arr.add(effective(e, defaults));
            return arr;
        }
        if (!variant.isJsonObject() || defaults == null) return variant;
        JsonObject merged = new JsonObject();
        for (var e : defaults.entrySet()) merged.add(e.getKey(), e.getValue());
        for (var e : variant.getAsJsonObject().entrySet()) {
            if (e.getKey().equals("textures") && merged.has("textures") && merged.get("textures").isJsonObject() && e.getValue().isJsonObject()) {
                JsonObject tex = merged.getAsJsonObject("textures").deepCopy();
                for (var t : e.getValue().getAsJsonObject().entrySet()) tex.add(t.getKey(), t.getValue());
                merged.add("textures", tex);
            } else {
                merged.add(e.getKey(), e.getValue());
            }
        }
        return merged;
    }

    private static boolean modelExists(String namespace, String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader.getResource("assets/" + namespace + "/models/" + path + ".json") != null;
    }

    /**
     * A 1.12 blockstate names its models relative to models/block/ ("model": "livingwood" is models/block/livingwood.json; names without a namespace
     * are vanilla's unless the mod has such a file); 1.13 and later name the full path. A variant that overrides textures gets its own generated
     * model file (modern blockstate variants have no texture overrides).
     */
    private static JsonElement blockModels(JsonElement variant, ResourceLocation blockId, String key, Map<String, Supplier<byte[]>> out) {
        if (variant.isJsonArray()) {
            var arr = new com.google.gson.JsonArray();
            int i = 0;
            for (JsonElement e : variant.getAsJsonArray()) arr.add(blockModels(e, blockId, key + "_" + i++, out));
            return arr;
        }
        if (!variant.isJsonObject()) return variant;
        String defaultNamespace = blockId.getNamespace();
        JsonObject copy = new JsonObject();
        JsonObject source = variant.getAsJsonObject();
        String model = null;
        for (var e : source.entrySet()) {
            String k = e.getKey();
            if (k.equals("model") && e.getValue().isJsonPrimitive()) {
                String m = e.getValue().getAsString().toLowerCase(java.util.Locale.ROOT);
                int colon = m.indexOf(':');
                String path = colon >= 0 ? m.substring(colon + 1) : m;
                String normalized = org.kylepitcock.legacyconnector.core.Ids.normalizePath(path);
                String ns = colon >= 0 ? m.substring(0, colon) : modelExists(defaultNamespace, "block/" + normalized) ? defaultNamespace : "minecraft";
                model = org.kylepitcock.legacyconnector.core.Ids.namespace(ns) + ":block/" + normalized;
            } else if (!k.equals("textures") && !k.equals("custom") && !k.equals("submodel") && !k.equals("transform") && !k.equals("simple_model")
                    && !k.equals("ambient_occlusion") && !k.equals("gui3d")) {
                copy.add(k, e.getValue());
            }
        }
        if (model == null) return copy;
        if (ObjAssets1122.isObj(model)) {
            // a Forge OBJ model: NeoForge's OBJ loader draws it (with the variant's texture overrides and flip-v)
            int c = model.indexOf(':');
            JsonObject textures = source.has("textures") && source.get("textures").isJsonObject() ? source.getAsJsonObject("textures") : null;
            copy.addProperty("model", ObjAssets1122.model(model.substring(0, c), model.substring(c + 1), textures, ObjAssets1122.custom(source),
                    textures == null ? null : blockId.getPath() + "_" + key, out));
            return copy;
        }
        if (source.has("textures") && source.get("textures").isJsonObject() && source.getAsJsonObject("textures").size() > 0) {
            JsonObject generated = new JsonObject();
            generated.addProperty("parent", model);
            JsonObject textures = new JsonObject();
            for (var t : source.getAsJsonObject("textures").entrySet()) {
                if (t.getValue().isJsonPrimitive()) textures.addProperty(t.getKey(), lowerRef(t.getValue().getAsString()));
            }
            generated.add("textures", textures);
            String path = "block/lc1122/" + blockId.getPath() + "_" + org.kylepitcock.legacyconnector.core.Ids.normalizePath(key);
            byte[] bytes = GSON.toJson(generated).getBytes(StandardCharsets.UTF_8);
            out.put("assets/" + blockId.getNamespace() + "/models/" + path + ".json", () -> bytes);
            model = blockId.getNamespace() + ":" + path;
        }
        copy.addProperty("model", model);
        return copy;
    }

    private static String lowerRef(String ref) {
        if (ref.startsWith("#")) return ref;
        int colon = ref.indexOf(':');
        String ns = colon < 0 ? "minecraft" : ref.substring(0, colon);
        String path = colon < 0 ? ref : ref.substring(colon + 1);
        return org.kylepitcock.legacyconnector.core.Ids.namespace(ns) + ":" + org.kylepitcock.legacyconnector.core.Ids.normalizePath(path);
    }

    /** The item models of a block's items (an ItemBlock without a model file of its own draws the block's model of its metadata). */
    private static void blockItemModels(Block block, ResourceLocation blockId, JsonObject state, Map<String, Supplier<byte[]>> out) {
        var item = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.itemOf(block);
        if (item == null || item.getRegistryName() == null) return;
        JsonObject variants = state.getAsJsonObject("variants");
        for (int meta = 0; meta < 16; meta++) {
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modern(item, meta);
            if (modern == null) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(modern);
            if (!itemId.getNamespace().equals(blockId.getNamespace()) || modelExists(itemId.getNamespace(), "item/" + itemId.getPath())) continue;
            JsonElement v = variants.get("meta=" + meta);
            while (v != null && v.isJsonArray() && v.getAsJsonArray().size() > 0) v = v.getAsJsonArray().get(0);
            if (v == null || !v.isJsonObject() || !v.getAsJsonObject().has("model")) continue;
            JsonObject model = new JsonObject();
            model.addProperty("parent", v.getAsJsonObject().get("model").getAsString());
            byte[] bytes = GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
            out.put("assets/" + itemId.getNamespace() + "/models/item/" + itemId.getPath() + ".json", () -> bytes);
        }
    }

    /**
     * A block whose states are mapped in code (ModelLoader.setCustomStateMapper, as Chisel does): the mapper names the blockstate file and
     * variant of each state.
     */
    private static JsonElement fromStateMapper(Block block, ResourceLocation id, IBlockState state) {
        var mapper = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$stateMapper(block);
        if (mapper == null) return null;
        try {
            var mrl = mapper.putStateModelLocations(block).get(state);
            if (mrl == null) return null;
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(org.kylepitcock.legacyconnector.core.Ids.namespace(mrl.getNamespace()),
                    org.kylepitcock.legacyconnector.core.Ids.normalizePath(mrl.getPath()));
            JsonObject original = readOriginal(file);
            if (original == null || !original.has("variants") || !original.get("variants").isJsonObject()) return null;
            JsonObject variants = original.getAsJsonObject("variants");
            JsonElement v = variants.get(mrl.getVariant());
            if (v == null && mrl.getVariant().equals("normal") && variants.size() == 1) v = variants.entrySet().iterator().next().getValue();
            if (v == null) return null;
            JsonObject defaults = original.has("defaults") && original.get("defaults").isJsonObject() ? original.getAsJsonObject("defaults") : null;
            return effective(v, defaults);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The variant of one state: the exact "a=b,c=d" key, "normal" for property-less blocks, or Forge's nested form. */
    private static JsonElement find(JsonObject legacyVariants, IBlockState state) {
        TreeMap<String, String> props = new TreeMap<>();
        for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            props.put(e.getKey().getName(), nameOf(e.getKey(), e.getValue()));
        }
        StringBuilder key = new StringBuilder();
        for (var e : props.entrySet()) {
            if (key.length() > 0) key.append(',');
            key.append(e.getKey()).append('=').append(e.getValue());
        }
        if (legacyVariants.has(key.toString())) return legacyVariants.get(key.toString());
        if (props.isEmpty() && legacyVariants.has("normal")) return legacyVariants.get("normal");
        // Forge also took the bare value of a block's only varying property as the variant name ("variants": {"array": ...} for variation=array)
        if (props.size() == 1 && legacyVariants.has(props.firstEntry().getValue()) && !legacyVariants.has(props.firstKey())) {
            return legacyVariants.get(props.firstEntry().getValue());
        }
        if (legacyVariants.has("normal") && legacyVariants.get("normal").isJsonObject() && !legacyVariants.has(props.firstKey())) {
            return legacyVariants.get("normal");
        }
        // nested form: variants {property: {value: model, ...}, ...}
        JsonObject merged = null;
        for (var e : props.entrySet()) {
            if (!legacyVariants.has(e.getKey()) || !legacyVariants.get(e.getKey()).isJsonObject()) continue;
            JsonObject values = legacyVariants.getAsJsonObject(e.getKey());
            if (!values.has(e.getValue()) || !values.get(e.getValue()).isJsonObject()) continue;
            if (merged == null) merged = new JsonObject();
            for (var field : values.getAsJsonObject(e.getValue()).entrySet()) merged.add(field.getKey(), field.getValue());
        }
        return merged;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String nameOf(IProperty property, Comparable<?> value) {
        return property.getName((Comparable) value);
    }

    private static JsonObject readOriginal(ResourceLocation id) {
        String path = "assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (var in = loader.getResourceAsStream(path)) {
            if (in == null) return null;
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
