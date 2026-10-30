package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2: the models of one blockstate variant (one object, or an array of weighted alternatives). */
public class VariantList {
    private final List<Variant> variantList;

    public VariantList(List<Variant> variantListIn) {
        this.variantList = variantListIn;
    }

    public List<Variant> getVariantList() {
        return variantList;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof VariantList other && variantList.equals(other.variantList);
    }

    @Override
    public int hashCode() {
        return variantList.hashCode();
    }

    /** A variant list from blockstate json: an object or an array of objects ({@code model}, {@code x}, {@code y}, {@code uvlock}, {@code weight}). */
    public static VariantList mcc$fromJson(JsonElement json) {
        List<Variant> list = new ArrayList<>();
        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            if (array.size() == 0) throw new JsonParseException("Empty variant array");
            for (JsonElement e : array) list.add(mcc$variant(e.getAsJsonObject()));
        } else {
            list.add(mcc$variant(json.getAsJsonObject()));
        }
        return new VariantList(list);
    }

    /** One variant: its model is {@code block/<model>} of the model's domain, as 1.12 resolved blockstate model names. */
    static Variant mcc$variant(JsonObject json) {
        String model = json.has("model") ? json.get("model").getAsString() : "builtin/missing";
        ResourceLocation rl = new ResourceLocation(model);
        ResourceLocation location = new ResourceLocation(rl.getNamespace(), "block/" + rl.getPath());
        int x = json.has("x") ? json.get("x").getAsInt() : 0;
        int y = json.has("y") ? json.get("y").getAsInt() : 0;
        ModelRotation rotation = ModelRotation.getModelRotation(x, y);
        if (rotation == null) throw new JsonParseException("Invalid BlockModelRotation x: " + x + ", y: " + y);
        boolean uvlock = json.has("uvlock") && json.get("uvlock").getAsBoolean();
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        if (weight < 1) throw new JsonParseException("Invalid weight " + weight + " found, expected integer >= 1");
        return new Variant(location, rotation, uvlock, weight);
    }
}
