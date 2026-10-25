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
            if (model != null) variants.add(modernKey, model);
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
