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
                JsonObject state = translate(block, id);
                if (state == null) continue;
                byte[] bytes = GSON.toJson(state).getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json", () -> bytes);
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
                if (target.equals(id.getNamespace() + ":item/" + id.getPath())) continue; // the mod's own file already has the id's name
                JsonObject model = new JsonObject();
                model.addProperty("parent", target);
                byte[] bytes = GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", () -> bytes);
            }
        }
    }

    private static JsonObject translate(Block block, ResourceLocation id) {
        JsonObject original = readOriginal(id);
        if (original == null) return null; // the mod models this block in code (or not at all): nothing to translate
        JsonObject legacyVariants = original != null && original.has("variants") && original.get("variants").isJsonObject() ? original.getAsJsonObject("variants") : null;
        JsonObject variants = new JsonObject();
        List<IBlockState> states = new ArrayList<>(block.getBlockState().getValidStates());
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
            if (model != null) variants.add(modernKey, blockModels(model, id.getNamespace()));
        }
        if (variants.size() == 0) {
            // no usable legacy file: a cube whose model is named like the block
            JsonObject m = new JsonObject();
            m.addProperty("model", id.getNamespace() + ":block/" + id.getPath());
            variants.add("meta=0", m);
        }
        JsonObject out = new JsonObject();
        out.add("variants", variants);
        return out;
    }

    /**
     * A 1.12 blockstate names its models relative to models/block/ ("model": "livingwood" is models/block/livingwood.json); 1.13 and later
     * name the full path, so the prefix is added.
     */
    private static JsonElement blockModels(JsonElement variant, String defaultNamespace) {
        if (variant.isJsonArray()) {
            var out = new com.google.gson.JsonArray();
            for (JsonElement e : variant.getAsJsonArray()) out.add(blockModels(e, defaultNamespace));
            return out;
        }
        if (!variant.isJsonObject()) return variant;
        JsonObject copy = new JsonObject();
        for (var e : variant.getAsJsonObject().entrySet()) {
            if (e.getKey().equals("model") && e.getValue().isJsonPrimitive()) {
                String m = e.getValue().getAsString().toLowerCase(java.util.Locale.ROOT);
                int colon = m.indexOf(':');
                String ns = colon >= 0 ? m.substring(0, colon) : defaultNamespace;
                String path = colon >= 0 ? m.substring(colon + 1) : m;
                copy.addProperty("model", org.kylepitcock.legacyconnector.core.Ids.namespace(ns) + ":block/" + org.kylepitcock.legacyconnector.core.Ids.normalizePath(path));
            } else {
                copy.add(e.getKey(), e.getValue());
            }
        }
        return copy;
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
