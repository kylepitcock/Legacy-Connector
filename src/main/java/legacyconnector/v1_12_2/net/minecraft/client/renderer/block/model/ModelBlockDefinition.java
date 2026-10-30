package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.multipart.Multipart;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 blockstate file: its {@code variants} by name and its {@code multipart} parts. Mods parse blockstates themselves with
 * {@link #parseFromReader} (CTM-style and connected-texture loaders). Forge's {@code forge_marker} format is not read here.
 */
public class ModelBlockDefinition {
    private final Map<String, VariantList> mapVariants = new LinkedHashMap<>();
    private Multipart multipartData;

    public static ModelBlockDefinition parseFromReader(Reader reader, ResourceLocation location) {
        try {
            JsonElement json = JsonParser.parseReader(reader);
            if (!json.isJsonObject()) throw new JsonParseException("Blockstate " + location + " is not a json object");
            return mcc$fromJson(json.getAsJsonObject());
        } catch (RuntimeException e) {
            throw new RuntimeException("Encountered an exception when loading model definition of '" + location + "' from: '" + location + "'", e);
        }
    }

    public ModelBlockDefinition(Map<String, VariantList> variants, Multipart multipart) {
        this.multipartData = multipart;
        this.mapVariants.putAll(variants);
    }

    /** The definitions of every resource pack merged: later packs replace variants by name and the multipart. */
    public ModelBlockDefinition(List<ModelBlockDefinition> parts) {
        ModelBlockDefinition last = null;
        for (ModelBlockDefinition def : parts) {
            if (def.hasMultipartData()) {
                mapVariants.clear();
                last = def;
            }
            mapVariants.putAll(def.mapVariants);
        }
        if (last != null) multipartData = last.multipartData;
    }

    public boolean hasVariant(String name) {
        return mapVariants.get(name) != null;
    }

    public VariantList getVariant(String name) {
        VariantList list = mapVariants.get(name);
        if (list == null) throw new MissingVariantException();
        return list;
    }

    public Map<String, VariantList> getVariants() {
        return mapVariants;
    }

    public boolean hasMultipartData() {
        return multipartData != null;
    }

    public Multipart getMultipartData() {
        return multipartData;
    }

    public Set<VariantList> getMultipartVariants() {
        Set<VariantList> set = new java.util.LinkedHashSet<>();
        for (VariantList list : mapVariants.values()) set.add(list);
        if (hasMultipartData()) set.addAll(multipartData.getVariants());
        return set;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ModelBlockDefinition other && mapVariants.equals(other.mapVariants)
                && (multipartData == null ? other.multipartData == null : multipartData.equals(other.multipartData));
    }

    @Override
    public int hashCode() {
        return 31 * mapVariants.hashCode() + (multipartData == null ? 0 : multipartData.hashCode());
    }

    /** A blockstate json object: {@code variants} (name to variant list) and/or {@code multipart}. */
    public static ModelBlockDefinition mcc$fromJson(JsonObject json) {
        Map<String, VariantList> variants = new LinkedHashMap<>();
        if (json.has("variants")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("variants").entrySet()) variants.put(e.getKey(), VariantList.mcc$fromJson(e.getValue()));
        }
        Multipart multipart = json.has("multipart") ? Multipart.mcc$fromJson(json.getAsJsonArray("multipart")) : null;
        if (variants.isEmpty() && (multipart == null || multipart.getSelectors().isEmpty())) throw new JsonParseException("Neither 'variants' nor 'multipart' found");
        return new ModelBlockDefinition(variants, multipart);
    }

    public class MissingVariantException extends RuntimeException {
        public MissingVariantException() {
        }
    }
}
